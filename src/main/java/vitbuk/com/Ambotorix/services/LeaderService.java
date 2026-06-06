package vitbuk.com.Ambotorix.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderService {

    private static final Logger log = LoggerFactory.getLogger(LeaderService.class);

    private volatile List<Leader> leaders;

    @Value("${data.dir:src/main/resources}")
    private String dataDir;

    @PostConstruct
    public void init() {
        this.leaders = loadLeaders();
    }

    // Package-private constructor for unit tests
    LeaderService(List<Leader> leaders) {
        this.dataDir = "src/main/resources";
        List<Leader> sorted = new ArrayList<>(leaders);
        sorted.sort(Comparator.comparing(Leader::getFullName));
        this.leaders = Collections.unmodifiableList(sorted);
    }

    public void reload() {
        this.leaders = loadLeaders();
    }

    private List<Leader> loadLeaders() {
        Gson gson = new Gson();
        String path = dataDir + "/civ6_leaders.json";
        try (FileReader reader = new FileReader(path)) {
            Type listType = new TypeToken<List<Leader>>() {}.getType();
            List<Leader> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                loaded.sort(Comparator.comparing(Leader::getFullName));
            }
            return loaded != null
                    ? Collections.unmodifiableList(loaded)
                    : Collections.emptyList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load leaders from " + path, e);
        }
    }

    public List<Leader> getLeaders() {
        return new ArrayList<>(leaders);
    }

    public List<String> getShortNames() {
        return getLeaders().stream()
                .map(Leader::getShortName)
                .collect(Collectors.toList());
    }

    public Lobby setLeadersPool(Lobby lobby) {
        List<Leader> nonBannedLeaders = getLeaders();
        if (!lobby.getBannedLeaders().isEmpty()) {
            nonBannedLeaders.removeAll(lobby.getBannedLeaders());
        }

        if (hasEnoughLeaders(nonBannedLeaders.size(), lobby.getPickSize(), lobby.getPlayers().size())) {
            lobby = setLeadersPoolToPlayers(nonBannedLeaders, lobby);
        } else {
            log.warn("Not enough leaders to assign unique pool to every player. pickSize={}, players={}, available={}",
                    lobby.getPickSize(), lobby.getPlayers().size(), nonBannedLeaders.size());
        }

        return lobby;
    }

    public String getShortNameMessage(Player player) {
        StringBuilder sb = new StringBuilder();
        for (Leader leader : player.getPicks()) {
            sb.append("/d_").append(leader.getShortName()).append(" ");
        }
        return sb.toString().trim();
    }

    public Leader getLeaderByShortName(String shortName) {
        if (leaders == null) return null;
        return leaders.stream()
                .filter(l -> l.getShortName().equalsIgnoreCase(shortName))
                .findFirst()
                .orElse(null);
    }

    private boolean hasEnoughLeaders(Integer notBannedLeaders, Integer pickSize, Integer playersAmount) {
        return notBannedLeaders > pickSize * playersAmount;
    }

    private Lobby setLeadersPoolToPlayers(List<Leader> leaders, Lobby lobby) {
        List<Leader> shuffledLeaders = new ArrayList<>(leaders);
        Collections.shuffle(shuffledLeaders, new Random());
        Iterator<Leader> leaderIterator = shuffledLeaders.iterator();

        for (Player p : lobby.getPlayers()) {
            List<Leader> pick = new ArrayList<>();
            for (int i = 0; i < lobby.getPickSize(); i++) {
                if (leaderIterator.hasNext()) {
                    pick.add(leaderIterator.next());
                }
            }
            p.setPicks(pick);
        }

        return lobby;
    }

    public String formatDescription(String description) {
        StringBuilder formattedText = new StringBuilder();
        String biasLine = "";

        String[] paragraphs = description.split("\n");
        List<String> contentParagraphs = new ArrayList<>();

        for (String line : paragraphs) {
            line = line.trim();

            if (line.contains("• Bias:")) {
                int biasIndex = line.indexOf("• Bias:");
                biasLine = "<i>" + line.substring(biasIndex).trim() + "</i>\n";
                line = line.substring(0, biasIndex).trim();
            }

            if (!line.isEmpty()) {
                contentParagraphs.add(line);
            }
        }

        if (!biasLine.isEmpty()) {
            formattedText.append(biasLine).append("\n");
        }

        for (int i = 0; i < contentParagraphs.size(); i++) {
            String paragraph = contentParagraphs.get(i);
            formattedText.append("<b>").append(paragraph).append("</b>\n");

            if (i + 1 < contentParagraphs.size()) {
                String nextParagraph = contentParagraphs.get(i + 1);
                String[] sentences = nextParagraph.split("(?<=[.:])\\s+");
                for (String sentence : sentences) {
                    formattedText.append(sentence.trim()).append("\n");
                }
                formattedText.append("\n");
                i++;
            }
        }

        return formattedText.toString().trim();
    }
}
