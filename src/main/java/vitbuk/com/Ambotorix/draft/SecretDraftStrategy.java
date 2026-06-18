package vitbuk.com.Ambotorix.draft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.PickImageGenerator;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import vitbuk.com.Ambotorix.services.AmbotorixService;
import vitbuk.com.Ambotorix.services.LeaderService;
import vitbuk.com.Ambotorix.services.MarkupService;

import java.util.Map;

@Component
public class SecretDraftStrategy implements DraftStrategy {

    private static final Logger log = LoggerFactory.getLogger(SecretDraftStrategy.class);
    private final LeaderService leaderService;
    private final MarkupService markupService;
    private final TelegramClient telegramClient;

    public SecretDraftStrategy(LeaderService leaderService, MarkupService markupService, TelegramClient telegramClient) {
        this.leaderService = leaderService;
        this.markupService = markupService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getName() { return "secret"; }

    @Override
    public void execute(Lobby lobby, Long chatId, AmbotorixService service) {
        leaderService.setLeadersPool(lobby);
        for (Player player : lobby.getPlayers()) {
            Long userId = player.getUserId();
            if (userId == null) {
                service.sendToChat(chatId, lobby.getMessageThreadId(),
                        "@" + player.getUserName() + " — couldn't send DM. Please message the bot directly first, then use <code>/pick [shortName]</code> in this chat.");
                continue;
            }
            PickImageGenerator.LeaderPickPhoto result = PickImageGenerator.createLeaderPickMessage(userId, player);
            result.sendPhoto().setReplyMarkup(markupService.pickMarkup(player.getPicks(), chatId));
            result.sendPhoto().setCaption("Your leaders — tap to pick one:");
            try {
                telegramClient.execute(result.sendPhoto());
            } catch (TelegramApiException e) {
                log.warn("Failed to DM player {} (userId={}): {}", player.getUserName(), userId, e.getMessage());
                service.sendToChat(chatId, lobby.getMessageThreadId(),
                        "@" + player.getUserName() + " — couldn't send DM. Please message the bot directly first, then use <code>/pick [shortName]</code> in this chat.");
            } finally {
                result.tempFile().delete();
            }
        }
        service.sendToChat(chatId, lobby.getMessageThreadId(), "Pick options sent to all players via DM. Use a pick button or <code>/pick [shortName]</code> in this chat.");
    }

    @Override
    public void onAllPicksIn(Lobby lobby, Long chatId, AmbotorixService service) {
        StringBuilder sb = new StringBuilder("🎉 All picks are in!\n\n");
        for (Map.Entry<String, Leader> entry : lobby.getPendingPicks().entrySet()) {
            sb.append("@").append(entry.getKey()).append(" → ").append(entry.getValue().getFullName()).append("\n");
        }
        service.sendToChat(chatId, lobby.getMessageThreadId(), sb.toString());
    }
}
