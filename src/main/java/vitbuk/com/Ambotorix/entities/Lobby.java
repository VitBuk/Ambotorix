package vitbuk.com.Ambotorix.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Lobby {

    private final LocalDateTime created;
    private final Player host;
    private List<CivMap> mapPool;
    private Integer banSize;
    private Integer pickSize;
    private List<Player> players;
    private CivMap selectedMap;

    public Lobby(Player host) {
        this.created = LocalDateTime.now();
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
    public List<String> getPlayersNames(){
        return players.stream()
                .map(Player::getUserName)
                .collect(Collectors.toList());
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

    public Player getPlayerByName(String userName) {
        return players.stream()
                .filter(p -> p.getUserName().equals(userName))
                .findFirst()
                .orElse(null);
    }

    public List<Leader> getBannedLeaders() {
        Set<Leader> bannedSet = new HashSet<>();
        for (Player player : players) {
            bannedSet.addAll(player.getBans());
        }

        if (bannedSet.size() == 0) {
            return new ArrayList<Leader>();
        }

        return bannedSet.stream().toList();
    }

    public boolean isBanned(Leader leader) {
        return getBannedLeaders().contains(leader);
    }

    public void addMap(CivMap civMap) {
        mapPool.add(civMap);
    }

    public boolean removeMap (CivMap civMap) {
        if (mapPool.contains(civMap)) {
            mapPool.remove(civMap);
            return true;
        }

        return false;
    }
    public void defaultSetup() {
        this.banSize = 1;
        this.pickSize = 6;
        this.mapPool = new ArrayList<>(CivMap.STANDARD_MAPS);
    }
}
