package vitbuk.com.Ambotorix.draft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.PickImageGenerator;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import vitbuk.com.Ambotorix.services.AmbotorixService;
import vitbuk.com.Ambotorix.services.LeaderService;
import vitbuk.com.Ambotorix.services.MarkupService;

@Component
public class OpenDraftStrategy implements DraftStrategy {

    private static final Logger log = LoggerFactory.getLogger(OpenDraftStrategy.class);
    private final LeaderService leaderService;
    private final MarkupService markupService;
    private final TelegramClient telegramClient;

    public OpenDraftStrategy(LeaderService leaderService, MarkupService markupService, TelegramClient telegramClient) {
        this.leaderService = leaderService;
        this.markupService = markupService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getName() { return "open"; }

    @Override
    public void execute(Lobby lobby, Long chatId, AmbotorixService service) {
        leaderService.setLeadersPool(lobby);
        for (Player player : lobby.getPlayers()) {
            service.sendToChat(chatId, "<b>" + player.getUserName() + ":</b>");
            PickImageGenerator.LeaderPickPhoto result = PickImageGenerator.createLeaderPickMessage(chatId, player);
            result.sendPhoto().setReplyMarkup(markupService.leadersMarkup(player.getPicks()));
            try {
                telegramClient.execute(result.sendPhoto());
            } catch (TelegramApiException e) {
                log.error("Failed to send pick image for player {}", player.getUserName(), e);
                throw new RuntimeException(e);
            } finally {
                result.tempFile().delete();
            }
        }
    }
}
