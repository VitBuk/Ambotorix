package vitbuk.com.Ambotorix.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.commands.*;
import vitbuk.com.Ambotorix.commands.structure.AdminCommand;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.GeneralCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.commands.structure.CommandFactory;
import vitbuk.com.Ambotorix.config.BotConfig;
import vitbuk.com.Ambotorix.draft.DraftStrategy;
import vitbuk.com.Ambotorix.draft.DraftStrategyFactory;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class    AmbotorixService {
    private static final Logger log = LoggerFactory.getLogger(AmbotorixService.class);

    private final TelegramClient telegramClient;
    private final LeaderService leaderService;
    private final LobbyService lobbyService;
    private final CommandFactory commandFactory;
    private final MarkupService markupService;
    private final BotConfig botConfig;
    private final DataUpdateService dataUpdateService;
    private final DraftStrategyFactory draftStrategyFactory;

    @Value("${data.dir:src/main/resources}")
    private String dataDir;

    @Autowired
    public AmbotorixService(TelegramClient telegramClient, LeaderService leaderService, LobbyService lobbyService, CommandFactory commandFactory, MarkupService markupService,
                            BotConfig botConfig, DataUpdateService dataUpdateService, DraftStrategyFactory draftStrategyFactory) {
        this.telegramClient = telegramClient;
        this.leaderService = leaderService;
        this.lobbyService = lobbyService;
        this.commandFactory = commandFactory;
        this.markupService = markupService;
        this.botConfig = botConfig;
        this.dataUpdateService = dataUpdateService;
        this.draftStrategyFactory = draftStrategyFactory;
    }

    //logic for command -> credits
    public void sendCredits(Update update) {
        sendMessage(update, "Bot is created by @VitBuk\nhttps://github.com/VitBuk");
    }

    //logic for command -> discord
    public void sendDiscord(Update update) {
        sendMessage(update, "Our discord server: \nJoin our Discord: https://discord.gg/2h425TExSt");
    }
    //logic for command -> /update
    public void sendUpdate(Update update) {
        sendMessage(update, "Checking for BBG updates...");
        String result = dataUpdateService.checkAndUpdate();
        sendMessage(update, result);
    }

    //logic for command -> help
    public void sendHelp(Update update) {
        List<Command> all = commandFactory.getAll();
        Comparator<Command> byPrefix = Comparator.comparing(c -> c.getInfo().prefix(), String.CASE_INSENSITIVE_ORDER);

        // General: /lobby, /lobbyInfo, /help, /leaders, alpha, /credits last
        List<Command> general = all.stream()
                .filter(c -> c instanceof GeneralCommand)
                .sorted((a, b) -> {
                    if (a instanceof LobbyCommand) return -1;
                    if (b instanceof LobbyCommand) return 1;
                    if (a instanceof LobbyInfoCommand) return -1;
                    if (b instanceof LobbyInfoCommand) return 1;
                    if (a instanceof HelpCommand) return -1;
                    if (b instanceof HelpCommand) return 1;
                    if (a instanceof LeadersCommand) return -1;
                    if (b instanceof LeadersCommand) return 1;
                    if (a instanceof CreditsCommand) return 1;
                    if (b instanceof CreditsCommand) return -1;
                    return a.getInfo().prefix().compareToIgnoreCase(b.getInfo().prefix());
                }).toList();

        List<Command> hostCmds = all.stream()
                .filter(c -> c instanceof HostCommand)
                .sorted((a, b) -> {
                    if (a instanceof StartCommand) return -1;
                    if (b instanceof StartCommand) return 1;
                    return a.getInfo().prefix().compareToIgnoreCase(b.getInfo().prefix());
                }).toList();

        List<Command> playerCmds = all.stream()
                .filter(c -> !(c instanceof GeneralCommand))
                .filter(c -> !(c instanceof AdminCommand))
                .filter(c -> !(c instanceof HostCommand))
                .sorted((a, b) -> {
                    if (a instanceof RegisterCommand) return -1;
                    if (b instanceof RegisterCommand) return 1;
                    return a.getInfo().prefix().compareToIgnoreCase(b.getInfo().prefix());
                }).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<b>General:</b>\n");
        general.forEach(c -> sb.append(c.getInfo().name()).append(" – ").append(c.getInfo().description()).append('\n'));

        sb.append("\n<b>Host commands:</b>\n");
        hostCmds.forEach(c -> sb.append(c.getInfo().name()).append(" – ").append(c.getInfo().description()).append('\n'));

        sb.append("\n<b>Player commands:</b>\n");
        playerCmds.forEach(c -> sb.append(c.getInfo().name()).append(" – ").append(c.getInfo().description()).append('\n'));

        sendMessage(update, sb.toString());
    }

    // logic for command -> /lobby
    public void sendLobby(Update update) {
        Long chatId = extractChatIdLong(update);
        Player host = new Player(update.getMessage().getFrom().getUserName(), update.getMessage().getFrom().getId());
        String message = lobbyService.createLobby(chatId, host);

        sendMessage(update, message);
    }

    // logic for command -> /leaders
    public void sendLeaders(Update update) {
        List<Leader> leaders = leaderService.getLeaders();

        InlineKeyboardMarkup markup = markupService.leadersMarkup(leaders);

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
            log.error("Failed to send leaders message: {}", e.getMessage(), e);
        }
    }

    //logic for command -> /mods
    public void sendMods(Update update) {
        sendMessage(update, allLines(dataDir + "/mods"));
    }

    //logic for command -> /settings
    public void sendSettings(Update update) {
        sendMessage(update, allLines(dataDir + "/settings"));
    }

    //logic for command -> /d_[shortName]
    public void sendDescription (Update update, String shortName){
        log.debug("sendDescription triggered for shortName: {}", shortName);

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
                    log.error("Failed to send leader photo: {}", e.getMessage(), e);
                }

                String formattedDescription = "<b>ShortName:</b> " + l.getShortName() + "\n\n"
                        + leaderService.formatDescription(l.getDescription());
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
            log.error("Failed to answer callback query: {}", e.getMessage(), e);
        }

        String dPrefix = commandFactory.infoOf(DescriptionCommand.class).prefix();
        String addPrefix = commandFactory.infoOf(MapAddCommand.class).prefix();

        if (data != null && data.startsWith(dPrefix)) {
            String shortName = data.substring(dPrefix.length()+1); // +1 is space or _ before shortname
            sendDescription(update, shortName);
        }

        if (data != null && data.startsWith(addPrefix)) {
            String payload = data.substring(addPrefix.length() + 1);
            String[] parts = payload.split(" ", 2);
            if (parts.length < 2) {
                sendMessage(update, "This button is outdated. Please use /maplist again.");
                return;
            }
            try {
                Long lobbyChatId = Long.parseLong(parts[0]);
                String civMapName = parts[1];
                sendMapAdd(update, CivMap.fromDisplayNameIgnoreCase(civMapName).get(), lobbyChatId);
            } catch (NumberFormatException e) {
                sendMessage(update, "This button is outdated. Please use /maplist again.");
            }
        }

        String pickPrefix = "/pick";
        if (data != null && data.startsWith(pickPrefix + " ")) {
            String payload = data.substring(pickPrefix.length() + 1);
            String[] parts = payload.split(" ", 2);
            Long lobbyChatId = Long.parseLong(parts[0]);
            String shortName = parts[1];
            sendPick(update, lobbyChatId, shortName);
        }
    }

    //logic for command -> /ban_[shortName]
    public void sendBan(Update update, String shortName) {
        Long chatId = extractChatIdLong(update);
        String userName = update.getMessage().getFrom().getUserName();
        Player player = lobbyService.findPlayerByName(chatId, userName);

        // registration check
        if (!lobbyService.isRegistered(chatId, userName)) {
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
        if (lobbyService.isBanned(chatId, leader)) {
            sendMessage(update, leader.getFullName() + " is already banned");
            return;
        }

        //has bans slots check
        if (!lobbyService.hasAvailableBans(chatId, player) && !isHost(update)) {
            sendMessage(update, "Player " + player.getUserName() + " cant ban more leaders");
            return;
        }

        player.getBans().add(leader);
        sendMessage(update, leader.getFullName() + " successfully banned by " + player.getUserName() + " .");

        StringBuilder sb = new StringBuilder("Bans: \n");
        for (Leader l: lobbyService.bannedLeaders(chatId)) {
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

        Long chatId = extractChatIdLong(update);
        Long userId = update.getMessage().getFrom().getId();
        String result = lobbyService.registerPlayer(chatId, update.getMessage().getFrom().getUserName(), userId);

        if (canDm(userId)) {
            sendMessage(update, result);
            sendToChat(userId, "You're registered for the lobby! Your leader picks will be sent here when the draft starts.");
        } else {
            sendMessage(update, result + "\n\n⚠️ To receive your picks in DM, please start a private chat with @" + botConfig.getUsername().replaceFirst("^@", "") + " first.");
        }
    }

    private boolean canDm(Long userId) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(userId)
                    .text("✅ DM confirmed — you'll receive your leader picks here when a draft starts.")
                    .build());
            return true;
        } catch (TelegramApiException e) {
            return false;
        }
    }

    //logic for command -> /start
    public void sendStart(Update update) {
        Long chatId = extractChatIdLong(update);
        Lobby lobby = lobbyService.getLobby(chatId);
        if (lobby == null) { sendNoLobby(update); return; }
        if (lobby.isDraftInProgress()) {
            sendMessage(update, "Draft is already in progress.");
            return;
        }
        lobby.setDraftInProgress(true);
        lobby.setDraftStartedAt(LocalDateTime.now());
        try {
            sendSlotOrder(update);
            sendRandomMap(update);
            DraftStrategy strategy = draftStrategyFactory.getStrategy(lobby.getDraftStrategyName());
            strategy.execute(lobby, chatId, this);
        } catch (Exception e) {
            log.error("Draft failed for chat {}, resetting draftInProgress", chatId, e);
            lobby.setDraftInProgress(false);
            lobby.setDraftStartedAt(null);
            sendBugReport(update);
        }
    }
    public void sendPick(Update update, Long lobbyChatId, String shortName) {
        Lobby lobby = lobbyService.getLobby(lobbyChatId);
        if (lobby == null) { sendMessage(update, "No active lobby."); return; }
        if (!lobby.isDraftInProgress()) { sendMessage(update, "No draft in progress."); return; }
        if (!"secret".equals(lobby.getDraftStrategyName())) {
            sendMessage(update, "/pick is only available in secret draft mode.");
            return;
        }

        String userName = update.hasCallbackQuery()
                ? update.getCallbackQuery().getFrom().getUserName()
                : update.getMessage().getFrom().getUserName();
        Long userChatId = update.hasCallbackQuery()
                ? update.getCallbackQuery().getFrom().getId()
                : update.getMessage().getFrom().getId();

        if (!lobbyService.isRegistered(lobbyChatId, userName)) {
            sendMessage(update, "You are not registered in this lobby.");
            return;
        }
        if (lobby.hasPendingPick(userName)) {
            sendMessage(update, "You already picked a leader.");
            return;
        }
        Leader leader = leaderService.getLeaderByShortName(shortName);
        if (leader == null) {
            sendMessage(update, "Unknown leader: " + shortName + ". Check your DM for available shortnames.");
            return;
        }
        Player player = lobbyService.findPlayerByName(lobbyChatId, userName);
        if (player == null) {
            sendMessage(update, "Player not found in lobby.");
            return;
        }
        if (!player.getPicks().contains(leader)) {
            sendMessage(update, "That leader is not in your pick pool.");
            return;
        }
        lobby.addPendingPick(userName, leader);
        sendToChat(userChatId, "You picked <b>" + leader.getFullName() + "</b>!");
        sendToChat(lobbyChatId, "@" + userName + " has made their pick. ("
                + lobby.getPendingPicks().size() + "/" + lobby.getPlayers().size() + ")");

        if (lobby.allPicksIn(lobby.getPlayers().size())) {
            draftStrategyFactory.getStrategy(lobby.getDraftStrategyName())
                    .onAllPicksIn(lobby, lobbyChatId, this);
            lobby.setDraftInProgress(false);
        }
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

        Long chatId = extractChatIdLong(update);
        List<CivMap> mapPool =  lobbyService.getMappool(chatId);

        if (mapPool == null) {
            sendBugReport(update);
            return;
        }

        StringBuilder sb = new StringBuilder("Map pool: \n");
        sb.append("<i>To remove map from map pool use ")
                .append(commandFactory.infoOf(MapRemoveCommand.class).name())
                .append(" command </i> \n");

        for (CivMap cm : mapPool) {
            sb.append(commandFactory.infoOf(MapRemoveCommand.class).prefix())
                    .append("_")
                    .append(cm.toString())
                    .append(" → ")
                    .append(cm.toString())
                    .append("\n");
        }

        sendMessage(update, sb.toString());
    }

    // Looks up the lobby by lobbyChatId but responds to the update's chat (e.g. a DM callback)
    public void sendMappool(Update update, Long lobbyChatId) {
        if (!lobbyService.hasLobby(lobbyChatId)) {
            sendNoLobby(update);
            return;
        }

        List<CivMap> mapPool = lobbyService.getMappool(lobbyChatId);
        if (mapPool == null) {
            sendBugReport(update);
            return;
        }

        StringBuilder sb = new StringBuilder("Map pool: \n");
        sb.append("<i>To remove map from map pool use ")
                .append(commandFactory.infoOf(MapRemoveCommand.class).name())
                .append(" command </i> \n");

        for (CivMap cm : mapPool) {
            sb.append(commandFactory.infoOf(MapRemoveCommand.class).prefix())
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
        List<CivMap> allMaps = Arrays.stream(CivMap.values()).toList();
        Long groupChatId = update.getMessage().getChatId();
        InlineKeyboardMarkup markup = markupService.maplistMarkup(allMaps, groupChatId);

        StringBuilder sb = new StringBuilder("Maps: \n");
        sb.append("<i>To add map to the pool use ")
                .append(commandFactory.infoOf(MapAddCommand.class).name())
                .append( " command </i> \n");

        SendMessage message  = SendMessage.builder()
                .chatId(update.getMessage().getFrom().getId())
                .text(sb.toString())
                .replyMarkup(markup)
                .parseMode("HTML")
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send maplist message: {}", e.getMessage(), e);
        }
    }

    //logic for command -> /mapAdd [name]
    public void sendMapAdd(Update update, CivMap civMap) {
        if (civMap == null) {
            sendMessage(update, "There is no such map. To get list of available maps use "
                    + commandFactory.infoOf(MapAddCommand.class).name()
                    + " command.");
            return;
        }

        Long chatId = extractChatIdLong(update);
        lobbyService.addMap(chatId, civMap);
        sendMappool(update);
    }

    // Called from DM callback — lobbyChatId is the group chat where the lobby lives
    public void sendMapAdd(Update update, CivMap civMap, Long lobbyChatId) {
        if (civMap == null) {
            sendMessage(update, "There is no such map. To get list of available maps use "
                    + commandFactory.infoOf(MapAddCommand.class).name()
                    + " command.");
            return;
        }
        lobbyService.addMap(lobbyChatId, civMap);
        sendMappool(update, lobbyChatId);
    }

    //logic for command /mapRemove [name]
    public void sendMapRemove(Update update, CivMap civMap) {
        String mappoolName = commandFactory.infoOf(MappoolCommand.class).name();

        if (civMap == null) {
            sendMessage(update, "There is no such map.Check map pool by using " + mappoolName + " command.");
            return;
        }

        Long chatId = extractChatIdLong(update);
        if (lobbyService.removeMap(chatId, civMap)) {
            sendMappool(update);
            return;
        }

        sendMessage(update, "There is no such map in map pool. Check map pool by using " + mappoolName + " command");
    }

    public void sendTerminate(Update update) {
        if (!hasLobby(update)) { sendNoLobby(update); return; }
        Long chatId = extractChatIdLong(update);
        lobbyService.removeLobby(chatId);
        sendMessage(update, "Lobby terminated by @" + update.getMessage().getFrom().getUserName() + ".");
    }
    public boolean isHost(Update update){
        Long chatId = extractChatIdLong(update);
        return lobbyService.isHost(chatId, update.getMessage().getFrom().getUserName());
    }

    public boolean hasLobby(Update update) {
        Long chatId = extractChatIdLong(update);
        return lobbyService.hasLobby(chatId);
    }

    public void sendNoLobby(Update update) {
        sendMessage(update, "Lobby is not up. Use command " + commandFactory.infoOf(LobbyCommand.class).name() + " to create new lobby");
    }

    public void sendNotAHost(Update update) {
        sendMessage(update, "You can`t use host commands");
    }

    public boolean isRegistered(Update update) {
        Long chatId = extractChatIdLong(update);
        return lobbyService.isRegistered(chatId, update.getMessage().getFrom().getUserName());
    }

    public void sendNotAPlayer(Update update) {
        sendMessage(update, "Unregistered players cant use that command. To register use: "  +
                commandFactory.infoOf(RegisterCommand.class).name() + " command");
    }

    public void sendNoSuchMap(Update update) {
        sendMessage(update, "No such map. Check " + commandFactory.infoOf(MaplistCommand.class).name()
                + " command for list of available maps");
    }

    private void sendSlotOrder(Update update) {
        Long chatId = extractChatIdLong(update);
        List<Player> shuffledPlayers = lobbyService.randomSlotOrder(chatId);
        if (shuffledPlayers == null || shuffledPlayers.isEmpty()) {
            sendMessage(update, "0 players registered");
            return;
        }

        StringBuilder sb = new StringBuilder("Slot order: \n");
        for (int i = 0; i < shuffledPlayers.size(); i++) {
            sb.append(i + 1)
                    .append(". ")
                    .append(shuffledPlayers.get(i).getUserName())
                    .append("\n");
        }

        sendMessage(update, sb.toString());
    }

    private void sendRandomMap(Update update) {
        Long chatId = extractChatIdLong(update);
        CivMap randomMap = lobbyService.randomMap(chatId);
        if (randomMap == null) {
            sendMessage(update, "There is no maps in the map pool");
            return;
        }

        sendMessage(update, "Map: " + randomMap.toString());
    }

    public void sendLobbyInfo(Update update) {
        Long chatId = extractChatIdLong(update);
        if (!lobbyService.hasLobby(chatId)) {
            sendMessage(update, "No active lobby. Use /lobby to create one.");
            return;
        }
        Lobby lobby = lobbyService.getLobby(chatId);

        String playerList = lobby.getPlayers().stream()
                .map(Player::getUserName)
                .collect(Collectors.joining(", "));
        String mapList = lobby.getMapPool().isEmpty() ? "none"
                : lobby.getMapPool().stream().map(CivMap::toString).collect(Collectors.joining(", "));
        String draftStatus = lobby.isDraftInProgress() ? "in progress" : "waiting";

        StringBuilder sb = new StringBuilder();
        sb.append("<b>Lobby by @").append(lobby.getHost().getUserName()).append("</b>")
                .append(" | Draft: ").append(lobby.getDraftStrategyName())
                .append(" | Status: ").append(draftStatus).append("\n")
                .append("Players (").append(lobby.getPlayers().size()).append("): ").append(playerList).append("\n")
                .append("Pick size: ").append(lobby.getPickSize())
                .append(" | Bans per player: ").append(lobby.getBanSize()).append("\n")
                .append("Map pool: ").append(mapList);

        if (lobby.getBanSize() > 0) {
            sb.append("\n\n<b>Bans:</b>");
            for (Player player : lobby.getPlayers()) {
                sb.append("\n@").append(player.getUserName()).append(": ");
                if (player.getBans().isEmpty()) {
                    sb.append("—");
                } else {
                    sb.append(player.getBans().stream()
                            .map(vitbuk.com.Ambotorix.entities.Leader::getFullName)
                            .collect(Collectors.joining(", ")));
                }
            }
        }

        sendMessage(update, sb.toString());
    }

    public void sendSetBanSize(Update update, int n) {
        if (n < 0) {
            sendMessage(update, "Ban size must be 0 or greater.");
            return;
        }
        Long chatId = extractChatIdLong(update);
        Lobby lobby = lobbyService.getLobby(chatId);
        if (lobby == null) { sendNoLobby(update); return; }
        lobby.setBanSize(n);
        sendMessage(update, "Ban size set to " + n + ".");
    }

    public void sendSetPickSize(Update update, int n) {
        if (n < 1) {
            sendMessage(update, "Pick size must be at least 1.");
            return;
        }
        Long chatId = extractChatIdLong(update);
        Lobby lobby = lobbyService.getLobby(chatId);
        if (lobby == null) { sendNoLobby(update); return; }
        lobby.setPickSize(n);
        sendMessage(update, "Pick size set to " + n + ".");
    }

    public void sendSetDraft(Update update, String strategyName) {
        if (!draftStrategyFactory.getStrategyNames().contains(strategyName)) {
            sendMessage(update, "Unknown strategy. Available: " + String.join(", ", draftStrategyFactory.getStrategyNames()));
            return;
        }
        Long chatId = extractChatIdLong(update);
        Lobby lobby = lobbyService.getLobby(chatId);
        if (lobby == null) { sendNoLobby(update); return; }
        lobby.setDraftStrategyName(strategyName);
        sendMessage(update, "Draft strategy set to: " + strategyName + ".");
    }

    public void sendAdminLobbies(Update update) {
        Map<Long, Lobby> all = lobbyService.getAllLobbies();
        if (all.isEmpty()) {
            sendMessage(update, "No active lobbies.");
            return;
        }
        StringBuilder sb = new StringBuilder("Active lobbies:\n");
        all.forEach((chatId, lobby) -> {
            long ageMinutes = Duration.between(lobby.getCreated(), LocalDateTime.now()).toMinutes();
            sb.append("chatId: ").append(chatId)
              .append(" | host: @").append(lobby.getHost().getUserName())
              .append(" | players: ").append(lobby.getPlayers().size())
              .append(" | age: ").append(ageMinutes).append("m\n");
        });
        sendMessage(update, sb.toString());
    }

    public void sendAdminTerminate(Update update, Long targetChatId) {
        if (!lobbyService.hasLobby(targetChatId)) {
            sendMessage(update, "No lobby found for chatId: " + targetChatId);
            return;
        }
        lobbyService.removeLobby(targetChatId);
        sendToChat(targetChatId, "Lobby terminated by bot admin.");
        sendMessage(update, "Lobby in chat " + targetChatId + " terminated.");
    }

    public void sendToChat(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void sendMessage(Update update, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(extractChatId(update))
                .text(text)
                .parseMode("HTML")
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
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
            log.error("Failed to send private message: {}", e.getMessage(), e);
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

    public Long extractChatIdLong(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return update.getMessage().getChatId();
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