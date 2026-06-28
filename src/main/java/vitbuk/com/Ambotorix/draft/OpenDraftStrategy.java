package vitbuk.com.Ambotorix.draft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import vitbuk.com.Ambotorix.PickImageGenerator;
import vitbuk.com.Ambotorix.entities.Lobby;
import vitbuk.com.Ambotorix.entities.Player;
import vitbuk.com.Ambotorix.services.AmbotorixService;
import vitbuk.com.Ambotorix.services.LeaderService;
import vitbuk.com.Ambotorix.services.MarkupService;

import java.io.File;

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
        String mapLine = lobby.getSelectedMap() != null
                ? "🗺 Map: " + lobby.getSelectedMap() + "\n\n"
                : "";
        for (Player player : lobby.getPlayers()) {
            PickImageGenerator.LeaderPickPhoto result = PickImageGenerator.createLeaderPickMessage(chatId, player);
            File tempFile = result.tempFile();
            try {
                // Group chat: name + image, no buttons
                service.sendToChat(chatId, "<b>" + player.getUserName() + ":</b>");
                telegramClient.execute(result.sendPhoto());
            } catch (TelegramApiException e) {
                log.error("Failed to send pick image for player {}", player.getUserName(), e);
                tempFile.delete();
                throw new RuntimeException(e);
            }

            // DM: same image with description buttons — non-fatal if it fails
            if (player.getUserId() != null) {
                try {
                    sendDmWithButtons(player, tempFile, mapLine);
                } catch (TelegramApiException e) {
                    log.warn("Could not send DM to player {} (userId={}): {}", player.getUserName(), player.getUserId(), e.getMessage());
                }
            } else {
                log.warn("No userId for player {}, skipping DM", player.getUserName());
            }
            tempFile.delete();
        }
    }

    private void sendDmWithButtons(Player player, File imageFile, String mapLine) throws TelegramApiException {
        SendPhoto dm = SendPhoto.builder()
                .chatId(player.getUserId())
                .photo(new InputFile(imageFile))
                .parseMode("HTML")
                .caption(mapLine + "Your leaders - tap to check descriptions:")
                .replyMarkup(markupService.leadersMarkup(player.getPicks()))
                .build();
        telegramClient.execute(dm);
    }
}
