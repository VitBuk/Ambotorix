package vitbuk.com.Ambotorix.services;

import org.springframework.stereotype.Service;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;

import java.time.LocalDateTime;

@Service
public class LobbyService {

    private Lobby lobby;

    public void createLobby(Player host) {
        if (lobby != null) {
            throw new IllegalStateException("Lobby already exists.");
        } else {
            this.lobby = new Lobby(LocalDateTime.now(), host);
        }
    }
}
