package vitbuk.com.Ambotorix.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.Constants;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Player;
import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AmbotorixService {
    private final TelegramClient telegramClient;
    private final LeaderService leaderService;
    private final LobbyService lobbyService;

    @Autowired
    public AmbotorixService(LeaderService leaderService, LobbyService lobbyService) {
        this.telegramClient = new OkHttpTelegramClient(Constants.BOT_TOKEN);
        this.leaderService = leaderService;
        this.lobbyService = lobbyService;
    }

    // logic for command -> /lobby
    public void sendLobby(Update update) {
        Player host = new Player(update.getMessage().getChat().getUserName());
        String message = lobbyService.createLobby(host);

        sendMessage(update, message);
    }

    // logic for command -> /leaders
    public void sendLeaders(Update update) {
        List<Leader> leaders = leaderService.getLeaders();

        StringBuilder message = new StringBuilder();
        message.append("Leaders: \n" );
        message.append("<i>To get description use " + CommandConstants.DESCRIPTION_NAME + " command or "
                + CommandConstants.DESCRIPTION_NAME2 + " command </i> \n");

        for (Leader l : leaders) {
            message.append("/d_").append(l.getShortName()).append(" → ").append(l.getFullName()).append("\n");
        }

        sendMessage(update, message.toString());
    }

    //logic for command -> /d_[shortName]
    public void sendDescription (Update update, String shortName){
        List<Leader> leaders = leaderService.getLeaders();

        for (Leader l : leaders) {
            if (l.getShortName().equalsIgnoreCase(shortName)) {
                SendPhoto photoMessage = SendPhoto.builder()
                        .chatId(update.getMessage().getChatId())
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
                sendMessage(update, formattedDescription);
                return;
            }
        }

        sendMessage(update, "Unknown leader. Use " + CommandConstants.LEADERS + " to see available description command");
    }

    //logic for command -> /ban_[shortName]
    public void sendBanLeader(Update update, String shortName) {
        Player player = lobbyService.findPlayerByName(update.getMessage().getChat().getUserName());

        // registration check
        if (!lobbyService.isRegistered(update.getMessage().getChat().getUserName())) {
            sendMessage(update, "Player " +
                    update.getMessage().getChat().getUserName() + " is not registered");
            return;
        }

        // leader exists check
        Leader leader = leaderService.getLeaderByShortName(shortName);
        if (leader == null) {
            sendMessage(update, "Unknown leader. User " + CommandConstants.LEADERS + " to see available description comamnd");
            return;
        }

        //already banned check
        if (lobbyService.isBanned(leader)) {
            sendMessage(update, "Leader " + leader.getFullName() + " is already banned");
            return;
        }

        //has bans slots check
        if (!lobbyService.hasAvailableBans(player)) {
            sendMessage(update, "Player " + player.getUserName() + " cant ban more leaders");
            return;
        }

        player.getBans().add(leader);
        sendMessage(update, "Leader " + leader.getFullName() + " successfully banned by " + player.getUserName() + " .");

        StringBuilder sb = new StringBuilder("Bans: \n");
        for (Leader l: lobbyService.bannedLeaders()) {
            sb.append(l.getFullName()).append(", ");
        }
        sendMessage(update, sb.toString());
    }

    // logic for unknown command
    public void sendUnknown(Update update) {
        sendMessage(update,"Unknown command. Use " + CommandConstants.HELP + " to get list of available commands." );
    }

    //logic for command -> /register
    public void sendRegister(Update update) {
        sendMessage(update, lobbyService.registerPlayer(update.getMessage().getChat().getUserName()));
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
        List<CivMap> mapPool =  lobbyService.getMappool();
        StringBuilder sb = new StringBuilder("Map pool of the lobby:");
        for (CivMap cm : mapPool) {
            sb.append(cm.toString());
        }

        sendMessage(update, sb.toString());
    }

    public boolean isHost(Update update){
        return lobbyService.isHost(update.getMessage().getChat().getUserName());
    }

    public void sendNotAHost(Update update) {
        sendMessage(update, "You can`t use host commands");
    }

    public boolean isRegistered(Update update) {
        return lobbyService.isRegistered(update.getMessage().getChat().getUserName());
    }

    public void sendNotAPlayer(Update update) {
        sendMessage(update, "Unregistered players cant use that command. To register use: "  +
                CommandConstants.REGISTER + " command");
    }

    private void sendMessage(Update update, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(text)
                .parseMode("HTML")
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
