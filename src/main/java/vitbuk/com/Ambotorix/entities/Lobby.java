package vitbuk.com.Ambotorix.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Lobby {

    private final LocalDateTime created;
    private final Player host;
    private List<CivMap> mapPool;
    private Integer banSize;
    private Integer pickSize;
    private List<Player> players;
    private CivMap selectedMap;

    public Lobby(LocalDateTime created, Player host) {
        this.created = created;
        this.host = host;
        players = new ArrayList<>();
        players.add(host);
        defaultSetup();
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public Player getHost() {
        return host;
    }

    public List<CivMap> getMapPool() {
        return mapPool;
    }

    public void setMapPool(List<CivMap> mapPool) {
        this.mapPool = mapPool;
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

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public CivMap getSelectedMap() {
        return selectedMap;
    }

    public void setSelectedMap(CivMap selectedMap) {
        this.selectedMap = selectedMap;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void defaultSetup() {
        this.banSize = 1;
        this.pickSize = 5;
        this.mapPool = CivMap.STANDARD_MAPS;
    }
}
