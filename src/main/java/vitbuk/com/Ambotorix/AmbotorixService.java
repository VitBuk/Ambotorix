package vitbuk.com.Ambotorix;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AmbotorixService {
    private final TelegramClient telegramClient;
    public AmbotorixService() {
        this.telegramClient = new OkHttpTelegramClient(Constants.BOT_TOKEN);
    }


    public Lobby setLeadersPoll (Lobby lobby) {
        List<Leader> nonBannedLeaders = getAllLeaders(Constants.LEADERS_JSON_PATH);
        if (!lobby.getBannedLeaders().isEmpty()) {
            nonBannedLeaders.removeAll(lobby.getBannedLeaders());
        }

        if (hasEnoughLeaders(nonBannedLeaders.size(), lobby.getPickSize(), lobby.getPlayers().size())) {
            lobby = setLeadersPollToPlayers(nonBannedLeaders, lobby);
        } else {
            System.out.println("Not enough leaders to get uniq poll to every player!");
        }

        return lobby;
    }

    public void createLobby(long chatId) {
        Lobby lobby = new Lobby("0", "lobby0", LocalDateTime.now());
        lobby.addPlayer(new Player("Player1"));
        lobby.addPlayer(new Player("Player2"));
        lobby.addPlayer(new Player("Player3"));
        lobby.addPlayer(new Player("Player4"));

        lobby.setPickSize(5);
        lobby = setLeadersPoll(lobby);

        for (Player p : lobby.getPlayers()) {
            SendPhoto sp = PickImageGenerator.createLeaderPickMessage(chatId, p);
            try {
                telegramClient.execute(sp);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void getLeadersList(long chatId) {
        List<Leader> leaders = getAllLeaders(Constants.LEADERS_JSON_PATH);

        StringBuilder message = new StringBuilder();
        message.append("Leaders: \n" );
        message.append("<i>To get description use /[shortName] command</i> \n");

        for (Leader l : leaders) {
            message.append("/").append(l.getShortName()).append(" → ").append(l.getFullName()).append("\n");
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


    private List<Leader> getAllLeaders(String leadersPath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(leadersPath)) {
            Type listType = new TypeToken<List<Leader>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean hasEnoughLeaders (Integer notBannedLeaders, Integer pickSize, Integer playersAmount) {
        return notBannedLeaders > pickSize * playersAmount;
    }

    private Lobby setLeadersPollToPlayers (List<Leader> leaders, Lobby lobby) {
        Collections.shuffle(leaders, new Random());
        Iterator<Leader> leaderIterator = leaders.iterator();

        for (Player p : lobby.getPlayers()) {
            List<Leader> pick = new ArrayList<>();
            for (int i=0; i<lobby.getPickSize(); i++) {
                if (leaderIterator.hasNext()) {
                    pick.add(leaderIterator.next());
                }
            }
            p.setPicks(pick);
        }

        return lobby;
    }
}
