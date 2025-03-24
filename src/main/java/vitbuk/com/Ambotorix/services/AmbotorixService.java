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
import vitbuk.com.Ambotorix.entities.Command;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Player;
import java.io.File;
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
//        Player player1 = new Player("Player1");
//        Lobby lobby = new Lobby(LocalDateTime.now(), player1);
//        lobby.addPlayer(new Player("Player2"));
//        lobby.addPlayer(new Player("Player3"));
//        lobby.addPlayer(new Player("Player4"));
//
//        lobby.setPickSize(5);
//        lobby = leaderService.setLeadersPool(lobby);
//
//        for (Player p : lobby.getPlayers()) {
//            String leaderShortNames = leaderService.getShortNameMessage(p);
//
//            sendMessage(chatId, leaderShortNames);
//
//            SendPhoto sp = PickImageGenerator.createLeaderPickMessage(chatId, p);
//            try {
//                telegramClient.execute(sp);
//            } catch (TelegramApiException e) {
//                e.printStackTrace();
//            }
//        }


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

        sendMessage(chatId, "Unknown leader. Use " + Command.LEADERS + " to see available description command");
    }

    //logic for command -> /ban_[shortName]
    public void sendBanLeader(long chatId, String userName, String shortName) {

    }

    // logic for unknown command
    public void sendUnknown(long chatId) {
        sendMessage(chatId,"Unknown command. Use " + Command.HELP + " to get list of available commands." );
    }

    //logic for command -> /register
    public void sendRegister(long chatId, String userName) {
        sendMessage(chatId, lobbyService.registerPlayer(userName));
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
