package vitbuk.com.Ambotorix.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Lobby {
    String id;
    String name;
    LocalDateTime created;
    Player host;
    List<Player> players;
    CivMap civMap;
    List<Leader> bannedLeaders;
    Integer pickSize;

    public Lobby(String id, String name, LocalDateTime created, Player host) {
        this.id = id;
        this.name = name;
        this.created = created;
        this.host = host;
        players = new ArrayList<>();
        bannedLeaders = new ArrayList<>();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public CivMap getCivMap() {
        return civMap;
    }

    public void setCivMap(CivMap civMap) {
        this.civMap = civMap;
    }

    public List<Leader> getBannedLeaders() {
        return bannedLeaders;
    }

    public void setBannedLeaders(List<Leader> bannedLeaders) {
        this.bannedLeaders = bannedLeaders;
    }

    public Integer getPickSize() {
        return pickSize;
    }

    public void setPickSize(Integer pickSize) {
        this.pickSize = pickSize;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }
}
