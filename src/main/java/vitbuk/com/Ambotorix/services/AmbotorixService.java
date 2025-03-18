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
import vitbuk.com.Ambotorix.PickImageGenerator;
import vitbuk.com.Ambotorix.entities.Command;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AmbotorixService {
    private final TelegramClient telegramClient;
    private final LeaderService leaderService;

    @Autowired
    public AmbotorixService(LeaderService leaderService) {
        this.telegramClient = new OkHttpTelegramClient(Constants.BOT_TOKEN);
        this.leaderService = leaderService;
    }

    // logic for command -> /lobby
    public void sendLobby(long chatId) {
        Lobby lobby = new Lobby("0", "lobby0", LocalDateTime.now());
        lobby.addPlayer(new Player("Player1"));
        lobby.addPlayer(new Player("Player2"));
        lobby.addPlayer(new Player("Player3"));
        lobby.addPlayer(new Player("Player4"));

        lobby.setPickSize(5);
        lobby = leaderService.setLeadersPool(lobby);

        for (Player p : lobby.getPlayers()) {
            String leaderShortNames = leaderService.getShortNameMessage(p);

            SendMessage sm = SendMessage
                    .builder()
                    .chatId(chatId)
                    .text(leaderShortNames)
                    .parseMode("HTML")
                    .build();

            try {
                telegramClient.execute(sm);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            SendPhoto sp = PickImageGenerator.createLeaderPickMessage(chatId, p);
            try {
                telegramClient.execute(sp);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
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

        SendMessage answer = SendMessage
                .builder()
                .chatId(chatId)
                .text(message.toString())
                .parseMode("HTML")
                .build();

        try{
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
                SendMessage sm = SendMessage.builder()
                        .chatId(chatId)
                        .text(formattedDescription)
                        .parseMode("HTML")
                        .build();

                try {
                    telegramClient.execute(sm);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                return;
            }
        }

        SendMessage errorSM = SendMessage
                .builder()
                .chatId(chatId)
                .text("Unknown leader. Use " + Command.LEADERS + " to see available description command")
                .build();

        try {
            telegramClient.execute(errorSM);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // logic for unknown command
    public void sendUnknown(long chatId) {
        sendMessage(chatId,"Unknown command. Use " + Command.HELP + " to get list of available commands." );
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
