package vitbuk.com.Ambotorix.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.Constants;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
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
    public void sendLobby(long chatId, String userName) {
        Player host = new Player(userName);
        String message = lobbyService.createLobby(host);

        sendMessage(chatId, message);
    }

    // logic for command -> /leaders
    public void sendLeaders(long chatId) {
        List<Leader> leaders = leaderService.getLeaders();

        StringBuilder message = new StringBuilder();
        message.append("Leaders: \n" );
        message.append("<i>To get description use /d_[shortName] command</i> \n");

        for (Leader l : leaders) {
            message.append("/d_").append(l.getShortName()).append(" → ").append(l.getFullName()).append("\n");
        }

        sendMessage(chatId, message.toString());
    }

    //logic for command -> /d_[shortName]
    public void sendDescription (long chatId, String shortName){
        List<Leader> leaders = leaderService.getLeaders();

        for (Leader l : leaders) {
            if (l.getShortName().equalsIgnoreCase(shortName)) {
                SendPhoto photoMessage = SendPhoto.builder()
                        .chatId(chatId)
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
                sendMessage(chatId, formattedDescription);
                return;
            }
        }

        sendMessage(chatId, "Unknown leader. Use " + CommandConstants.LEADERS + " to see available description command");
    }

    //logic for command -> /ban_[shortName]
    public void sendBanLeader(long chatId, String userName, String shortName) {
        Player player = lobbyService.findPlayerByName(userName);

        // registration check
        if (player == null) {
            sendMessage(chatId, "Player " + userName + " is not registered");
            return;
        }

        // leader exists check
        Leader leader = leaderService.getLeaderByShortName(shortName);
        if (leader == null) {
            sendMessage(chatId, "Unknown leader. User " + CommandConstants.LEADERS + " to see available description comamnd");
            return;
        }

        //already banned check
        if (lobbyService.isBanned(leader)) {
            sendMessage(chatId, "Leader " + leader.getFullName() + " is already banned");
            return;
        }

        //has bans slots check
        if (!lobbyService.hasAvailableBans(player)) {
            sendMessage(chatId, "Player " + player.getUserName() + " cant ban more leaders");
            return;
        }

        player.getBans().add(leader);
        sendMessage(chatId, "Leader " + leader.getFullName() + " successfully banned by " + player.getUserName() + " .");

        StringBuilder sb = new StringBuilder("Bans: \n");
        for (Leader l: lobbyService.bannedLeaders()) {
            sb.append(l.getFullName()).append(", ");
        }
        sendMessage(chatId, sb.toString());
    }

    // logic for unknown command
    public void sendUnknown(long chatId) {
        sendMessage(chatId,"Unknown command. Use " + CommandConstants.HELP + " to get list of available commands." );
    }

    //logic for command -> /register
    public void sendRegister(long chatId, String userName) {
        sendMessage(chatId, lobbyService.registerPlayer(userName));
    }

    //logic for command -> /time
    public void sendTime(Long chatId) {
        ZonedDateTime nowInRiga = ZonedDateTime.now(ZoneId.of("Europe/Riga"));
        ZonedDateTime nowInMunich = ZonedDateTime.now(ZoneId.of("Europe/Berlin"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss, dd MMM yyyy");

        String message = String.format("\uD83C\uDF07 Current time:\n"
                        + "\uD83C\uDDF1\uD83C\uDDFB Riga: %s\n"
                        + "\uD83C\uDDE9\uD83C\uDDEA Munich: %s",
                nowInRiga.format(formatter),
                nowInMunich.format(formatter)
        );

        sendMessage(chatId, message);
    }
    private void sendMessage(long chatId, String text) {
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
}
