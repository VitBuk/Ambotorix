package vitbuk.com.Ambotorix.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.Constants;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

@Service
public class LeaderService {
    private final List<Leader> leaders;

    public LeaderService(List<Leader> leaders) {
        this.leaders = loadLeaders();
    }

    private List<Leader> loadLeaders() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(Constants.LEADERS_JSON_PATH)) {
            Type listType = new TypeToken<List<Leader>>() {}.getType();
            List<Leader> loadedLeaders = gson.fromJson(reader, listType);
            if (loadedLeaders != null) {
                loadedLeaders.sort(Comparator.comparing(Leader::getFullName));
            }
            return Collections.unmodifiableList(loadedLeaders);

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Leader> getLeaders() {return new ArrayList<>(leaders);}

    public Lobby setLeadersPool(Lobby lobby) {
        List<Leader> nonBannedLeaders = getLeaders();
        if (!lobby.getBannedLeaders().isEmpty()) {
            nonBannedLeaders.removeAll(lobby.getBannedLeaders());
        }

        if (hasEnoughLeaders(nonBannedLeaders.size(), lobby.getPickSize(), lobby.getPlayers().size())) {
            lobby = setLeadersPoolToPlayers(nonBannedLeaders, lobby);
        } else {
            System.out.println("Not enough leaders to get uniq poll to every player!");
        }

        return lobby;
    }

    public String getShortNameMessage(Player player) {
        StringBuilder sb = new StringBuilder("<b>").append(player.getName()).append(":</b>").append("\n");
        for (Leader leader : player.getPicks()) {
            sb.append("/").append(leader.getShortName()).append(" ");
        }

        return sb.toString().trim();
    }

    private boolean hasEnoughLeaders (Integer notBannedLeaders, Integer pickSize, Integer playersAmount) {
        return notBannedLeaders > pickSize * playersAmount;
    }

    private Lobby setLeadersPoolToPlayers(List<Leader> leaders, Lobby lobby) {
        List<Leader> shuffledLeaders = getLeaders();
        Collections.shuffle(shuffledLeaders, new Random());
        Iterator<Leader> leaderIterator = shuffledLeaders.iterator();

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
