package vitbuk.com.Ambotorix.entities;

import java.util.Date;
import java.util.List;

public class Lobby {
    String id;
    String name;
    Date created;
    List<Player> players;
    CivMap civMap;
    List<Leader> bannedLeaders;

    public Lobby(String id, String name, Date created) {
        this.id = id;
        this.name = name;
        this.created = created;
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
}
