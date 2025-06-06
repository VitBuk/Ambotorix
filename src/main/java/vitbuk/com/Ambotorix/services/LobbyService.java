package vitbuk.com.Ambotorix.services;

import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;

import java.util.Collections;
import java.util.List;

@Service
public class LobbyService {

    private Lobby lobby;

    public String createLobby(Player host) {
        if (lobby != null) {
            return "Lobby is already exist. " + lobby.getHost().getUserName() + " can terminate it by using /terminate command";
        } else {
            this.lobby = new Lobby(host);
            StringBuilder sb = new StringBuilder();
            sb.append("Lobby is created by ").append(lobby.getHost().getUserName()).append("\n");
            // TODO: list of commands host can use after lobby creation
            return sb.toString();
        }
    }

    public String registerPlayer(String userName) {
        if (lobby.getPlayersNames().contains(userName)) {
            return "Player " + userName + " is already registered";
        }
        Player player = new Player(userName);
        lobby.addPlayer(player);

        return "Player " + player.getUserName() + " added to lobby";
    }

    public boolean isRegistered(String userName) {
        return lobby.getPlayersNames().contains(userName);
    }
    public boolean isBanned(Leader leader) {
        return lobby.getBannedLeaders().contains(leader);
    }

    public boolean hasAvailableBans(Player player) {
        return lobby.getBanSize() > player.getBans().size();
    }

    public List<Leader> bannedLeaders() {
        return lobby.getBannedLeaders();
    }

    public boolean isHost(String userName) {
        if (lobby == null) {

        }
        String hostUserName = lobby.getHost().getUserName();
        return hostUserName != null && hostUserName.equalsIgnoreCase(userName);
    }
    public Player findPlayerByName(String userName){
        if (lobby == null || lobby.getPlayers() == null) {
            return null;
        }

        return lobby.getPlayers()
                .stream()
                .filter(p -> p.getUserName().equalsIgnoreCase(userName))
                .findFirst()
                .orElse(null);
    }

    public List<CivMap> getMappool() {
        if (lobby == null || lobby.getMapPool() == null) {
            return null;
        }

        return lobby.getMapPool();
    }

    public void addMap(CivMap civMap) {
        lobby.addMap(civMap);
    }

    public boolean removeMap (CivMap civMap) {
        return  lobby.removeMap(civMap);
    }

    public List<Player> randomSlotOrder() {
        Collections.shuffle(lobby.getPlayers());
        return lobby.getPlayers();
    }

    public CivMap randomMap() {
        Collections.shuffle(lobby.getMapPool());
        return lobby.getMapPool().get(0);
    }

    public Lobby getLobby() {
        return lobby;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }

}
