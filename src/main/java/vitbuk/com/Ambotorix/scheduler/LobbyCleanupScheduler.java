package vitbuk.com.Ambotorix.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vitbuk.com.Ambotorix.services.AmbotorixService;
import vitbuk.com.Ambotorix.services.LobbyService;

import java.util.List;

@Component
public class LobbyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LobbyCleanupScheduler.class);

    private final LobbyService lobbyService;
    private final AmbotorixService ambotorixService;

    @Value("${lobby.auto-terminate.hours:4}")
    private int autoTerminateHours;

    public LobbyCleanupScheduler(LobbyService lobbyService, AmbotorixService ambotorixService) {
        this.lobbyService = lobbyService;
        this.ambotorixService = ambotorixService;
    }

    @Scheduled(fixedRate = 900_000) // every 15 minutes
    public void terminateExpiredLobbies() {
        List<Long> expired = lobbyService.getExpiredLobbyChatIds(autoTerminateHours);
        for (Long chatId : expired) {
            log.info("Auto-terminating lobby in chat {} ({}h after /start)", chatId, autoTerminateHours);
            lobbyService.removeLobby(chatId);
            ambotorixService.sendToChat(chatId,
                "Lobby automatically terminated after " + autoTerminateHours + " hours.");
        }
    }
}
