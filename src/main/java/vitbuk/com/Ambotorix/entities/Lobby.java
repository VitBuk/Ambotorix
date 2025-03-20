package vitbuk.com.Ambotorix.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Lobby {

    private final LocalDateTime created;
    private final Player host;
    private List<Player> players;
    private CivMap civMap;
    private List<Leader> bannedLeaders;
    private Integer banSize;
    private Integer pickSize;

    public Lobby(LocalDateTime created, Player host) {
        this.created = created;
        this.host = host;
        players = new ArrayList<>();
        bannedLeaders = new ArrayList<>();
        players.add(host);
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public Player getHost() {
        return host;
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

    public Integer getBanSize() {
        return banSize;
    }

    public void setBanSize(Integer banSize) {
        this.banSize = banSize;
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
