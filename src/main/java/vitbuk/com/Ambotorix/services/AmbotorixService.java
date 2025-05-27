package vitbuk.com.Ambotorix.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.Constants;
import vitbuk.com.Ambotorix.PickImageGenerator;
import vitbuk.com.Ambotorix.commands.*;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandFactory;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AmbotorixService {
    private final TelegramClient telegramClient;
    private final LeaderService leaderService;
    private final LobbyService lobbyService;
    private final CommandFactory commandFactory;

    @Autowired
    public AmbotorixService(LeaderService leaderService, LobbyService lobbyService, CommandFactory commandFactory) {
        this.telegramClient = new OkHttpTelegramClient(Constants.BOT_TOKEN);
        this.leaderService = leaderService;
        this.lobbyService = lobbyService;
        this.commandFactory = commandFactory;
    }

    //logic for command -> credits
    public void sendCredits(Update update) {
        sendMessage(update, "Bot is created by" + Constants.CREDITS_NAME + "\n" + Constants.CREDITS_GITHUB);
    }

    //logic for command -> discord
    public void sendDiscord(Update update) {
        sendMessage(update, "Our discord server: \n" + Constants.DISCORD);
    }
    //logic for command -> help
    public void sendHelp(Update update) {
        List<Command> commands = commandFactory.getAll()
                .stream()
                .sorted(Comparator.comparing(c -> c.getInfo().prefix(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        StringBuilder sb = new StringBuilder("Commands:\n");
        for (Command c : commands) {
            CommandInfo info = c.getInfo();
            sb.append(info.name())
                    .append(" – ")
                    .append(info.description())
                    .append('\n');
        }

        sendMessage(update, sb.toString());
    }

    // logic for command -> /lobby
    public void sendLobby(Update update) {
        Player host = new Player(update.getMessage().getFrom().getUserName());
        String message = lobbyService.createLobby(host);

        sendMessage(update, message);
    }

    // logic for command -> /leaders
    public void sendLeaders(Update update) {
        List<Leader> leaders = leaderService.getLeaders();

        InlineKeyboardMarkup markup = leadersMarkup(leaders);

        StringBuilder sb = new StringBuilder("Leaders: \n");
        sb.append("<i>To get description use /d_[shortName] \n")
                .append("command or buttons below: </i>");

        SendMessage message  = SendMessage.builder()
                .chatId(update.getMessage().getFrom().getId())
                .text(sb.toString())
                .replyMarkup(markup)
                .parseMode("HTML")
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //logic for command -> /mods
    public void sendMods(Update update) {
        sendMessage(update, allLines(Constants.MODS_PATH));
    }

    //logic for command -> /settings
    public void sendSettings(Update update) {
        sendMessage(update, allLines(Constants.SETTINGS_PATH));
    }

    //logic for command -> /d_[shortName]
    public void sendDescription (Update update, String shortName){
        List<Leader> leaders = leaderService.getLeaders();

        for (Leader l : leaders) {
            if (l.getShortName().equalsIgnoreCase(shortName)) {
                SendPhoto photoMessage = SendPhoto.builder()
                        .chatId(extractPrivateChatId(update))
                        .photo(new InputFile(new File(l.getPicPath())))
                        .caption("<b>" + l.getFullName() + "</b>")
                        .parseMode("HTML")
                        .build();

                try {
                    telegramClient.execute(photoMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                String formattedDescription = leaderService.formatDescription(l.getDescription());
                sendPrivateMessage(update, formattedDescription);
                return;
            }
        }

        sendPrivateMessage(update, "Unknown leader. Use " + commandFactory.infoOf(LeadersCommand.class).name() + " to see available description command");
    }

    public void makeCallbackQuery(Update update){
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        try {
            telegramClient.execute(
                    AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackQuery.getId())
                            .build()
            );
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        String dPrefix = commandFactory.infoOf(DescriptionCommand.class).prefix();

        if (data != null && data.startsWith(dPrefix)) {
            String shortName = data.substring(dPrefix.length());
            sendDescription(update, shortName);
        }
    }

    //logic for command -> /ban_[shortName]
    public void sendBan(Update update, String shortName) {
        String userName = update.getMessage().getFrom().getUserName();
        Player player = lobbyService.findPlayerByName(userName);

        // registration check
        if (!lobbyService.isRegistered(userName)) {
            sendMessage(update, "Player " + userName + " is not registered");
            return;
        }

        // leader exists check
        Leader leader = leaderService.getLeaderByShortName(shortName);
        if (leader == null) {
            sendMessage(update, "Unknown leader. User " + commandFactory.infoOf(LeadersCommand.class).name() + " to see available description comamnd");
            return;
        }

        //already banned check
        if (lobbyService.isBanned(leader)) {
            sendMessage(update, leader.getFullName() + " is already banned");
            return;
        }

        //has bans slots check
        if (!lobbyService.hasAvailableBans(player) && !isHost(update)) {
            sendMessage(update, "Player " + player.getUserName() + " cant ban more leaders");
            return;
        }

        player.getBans().add(leader);
        sendMessage(update, leader.getFullName() + " successfully banned by " + player.getUserName() + " .");

        StringBuilder sb = new StringBuilder("Bans: \n");
        for (Leader l: lobbyService.bannedLeaders()) {
            sb.append(l.getFullName()).append(", ");
        }
        sendMessage(update, sb.toString());
    }

    // logic for unknown command
    public void sendUnknown(Update update) {
        sendMessage(update,"Unknown command. Use " + commandFactory.infoOf(HelpCommand.class).name() + " to get list of available commands." );
    }

    //logic for command -> /register
    public void sendRegister(Update update) {
        if (!hasLobby(update)) {
            sendNoLobby(update);
            return;
        }

        sendMessage(update, lobbyService.registerPlayer(update.getMessage().getFrom().getUserName()));
    }

    //logic for command -> /start
    public void sendStart(Update update) {
        sendSlotOrder(update);
        sendRandomMap(update);
        sendPicks(update);
    }
    //logic for command -> /time
    public void sendTime(Update update) {
        ZonedDateTime nowInRiga = ZonedDateTime.now(ZoneId.of("Europe/Riga"));
        ZonedDateTime nowInMunich = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss, dd MMM yyyy");

        String message = String.format("\uD83C\uDF07 Current time:\n"
                        + "\uD83C\uDDF1\uD83C\uDDFB Riga: %s\n"
                        + "\uD83C\uDDE9\uD83C\uDDEA Munich: %s",
                nowInRiga.format(formatter),
                nowInMunich.format(formatter)
        );

        sendMessage(update, message);
    }

    //logic for command -> /mappool
    public void sendMappool(Update update) {
        if (!hasLobby(update)) {
            sendNoLobby(update);
            return;
        }

        List<CivMap> mapPool =  lobbyService.getMappool();

        if (mapPool == null) {
            sendBugReport(update);
        }
        
        StringBuilder sb = new StringBuilder("Map pool: \n");
        sb.append("<i>To remove map from map pool use ")
                .append(commandFactory.infoOf(MapRemoveCommand.class).name())
                .append(" command </i> \n");

        for (CivMap cm : mapPool) {
            sb.append(commandFactory.infoOf(MapRemoveCommand.class).name())
                    .append("_")
                    .append(cm.toString())
                    .append(" → ")
                    .append(cm.toString())
                    .append("\n");
        }

        sendMessage(update, sb.toString());
    }

    //logic for the command -> maplist
    public void sendMaplist(Update update) {
        StringBuilder sb = new StringBuilder("Maps: \n");
        sb.append("<i>To add map to the pool use ")
                .append(commandFactory.infoOf(MapAddCommand.class).name())
                .append( " command </i> \n");

        for (CivMap cm : CivMap.values()) {
            sb.append(commandFactory.infoOf(MapAddCommand.class).name())
                    .append("_")
                    .append(cm.toString())
                    .append(" → ")
                    .append(cm.toString())
                    .append("\n");
        }

        sendPrivateMessage(update, sb.toString());
    }

    //logic for command -> /mapAdd [name]
    public void sendMapAdd(Update update, CivMap civMap) {
        if (civMap == null) {
            sendMessage(update, "There is no such map. To get list of available maps use "
                    + commandFactory.infoOf(MapAddCommand.class).name()
                    + " command.");
            return;
        }

        lobbyService.addMap(civMap);
        sendMappool(update);
    }

    //logic for command /mapRemove [name]
    public void sendMapRemove(Update update, CivMap civMap) {
        String mappoolName = commandFactory.infoOf(MappoolCommand.class).name();

        if (civMap == null) {
            sendMessage(update, "There is no such map.Check map pool by using " + mappoolName + " command.");
            return;
        }

        if (lobbyService.removeMap(civMap)) {
            sendMappool(update);
            return;
        }

        sendMessage(update, "There is no such map in map pool. Check map pool by using " + mappoolName + " command");
    }
    public boolean isHost(Update update){
        return lobbyService.isHost(update.getMessage().getFrom().getUserName());
    }

    public boolean hasLobby(Update update) {
        return lobbyService.getLobby() != null;
    }

    public void sendNoLobby(Update update) {
        sendMessage(update, "Lobby is not up. Use command " + commandFactory.infoOf(LobbyCommand.class).name() + " to create new lobby");
    }

    public void sendNotAHost(Update update) {
        sendMessage(update, "You can`t use host commands");
    }

    public boolean isRegistered(Update update) {
        return lobbyService.isRegistered(update.getMessage().getFrom().getUserName());
    }

    public void sendNotAPlayer(Update update) {
        sendMessage(update, "Unregistered players cant use that command. To register use: "  +
                commandFactory.infoOf(RegisterCommand.class).name() + " command");
    }

    private void sendSlotOrder(Update update) {
        List<Player> shuffledPlayers = lobbyService.randomSlotOrder();
        if (shuffledPlayers == null ) {
            sendMessage(update, "0 players registered");
        }

        StringBuilder sb = new StringBuilder("Slot order: \n");
        for (int i=0; i<shuffledPlayers.size(); i++) {
            sb.append(i)
                    .append(". ")
                    .append(shuffledPlayers.get(i).getUserName());
        }

        sendMessage(update, sb.toString());
    }

    private void sendRandomMap(Update update) {
        CivMap randomMap = lobbyService.randomMap();
        if (randomMap == null ) {
            sendMessage(update, "There is no maps in the map pool");
        }

        sendMessage(update, "Map: " + randomMap.toString());
    }

    private void sendPicks(Update update) {
        Lobby lobby = lobbyService.getLobby();
        lobby = leaderService.setLeadersPool(lobby);

        for (Player p : lobby.getPlayers()) {
            sendMessage(update, "<b>" + p.getUserName() + ":</b>");
            SendPhoto sendPhoto = PickImageGenerator.createLeaderPickMessage(Long.valueOf(extractChatId(update)), p);
            InlineKeyboardMarkup markup = leadersMarkup(p.getPicks());
            sendPhoto.setReplyMarkup(markup);

            try{
                telegramClient.execute(sendPhoto);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private InlineKeyboardMarkup leadersMarkup(List<Leader> leaders) {
        String dPrefix = commandFactory.infoOf(DescriptionCommand.class).prefix();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (Leader l : leaders) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(l.getFullName())
                    .callbackData(dPrefix + l.getShortName())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    private InlineKeyboardMarkup mapsMarkup(List<CivMap> maps, Command command) {
        String mPrefix = commandFactory.infoOf(command.getClass()).prefix();

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (CivMap m : maps) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(m.name())
                    .callbackData(mPrefix + m.name())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    private void sendMessage(Update update, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(extractChatId(update))
                .text(text)
                .parseMode("HTML")
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendBugReport(Update update) {
        sendMessage(update, "Something wrong happened. Please contact @VitBuk to report bug.");
    }

    private void sendPrivateMessage(Update update, String text) {
        String chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getFrom().getId().toString();
        } else {
            chatId = update.getMessage().getFrom().getId().toString();
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String extractPrivateChatId( Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getFrom()
                    .getId()
                    .toString();
        }

        return update.getMessage()
                .getFrom()
                .getId()
                .toString();
    }
    private String extractChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getMessage()
                    .getChatId()
                    .toString();
        }
        return update.getMessage()
                .getChatId()
                .toString();
    }

    private List<String> readLines (String filePath) {
        try {
            return Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String allLines (String path) {
        List<String> lines = readLines(path);
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s);
            sb.append("\n");
        }

        return sb.toString();
    }
}