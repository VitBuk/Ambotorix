package vitbuk.com.Ambotorix.services;

import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;

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

    public boolean isBanned(String shortName) {
        return lobby.getBannedLeaders().stream()
                .anyMatch(leader -> leader.getShortName().equalsIgnoreCase(shortName));
    }

    public boolean isRegistered(String userName) {
        return lobby.getPlayers().stream()
                .anyMatch(player -> player.getUserName().equals(userName));
    }
}
