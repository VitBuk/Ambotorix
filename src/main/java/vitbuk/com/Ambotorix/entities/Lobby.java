package vitbuk.com.Ambotorix.entities;

import java.util.Date;

public class Lobby {
    String id;
    String name;
    Date created;
    List<Player> players;
    Enum<civMap> civMap;
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

    public Enum<civMap> getCivMap() {
        return civMap;
    }

    public void setCivMap(Enum<civMap> civMap) {
        this.civMap = civMap;
    }

    public List<Leader> getBannedLeaders() {
        return bannedLeaders;
    }

    public void setBannedLeaders(List<Leader> bannedLeaders) {
        this.bannedLeaders = bannedLeaders;
    }
}
