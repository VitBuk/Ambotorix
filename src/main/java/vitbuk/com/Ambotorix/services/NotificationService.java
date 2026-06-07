package vitbuk.com.Ambotorix.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final TelegramClient telegramClient;

    @Value("${bot.admin.id}")
    private Long adminTelegramId;

    public NotificationService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public void notifyError(String subject, String body) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(adminTelegramId)
                    .text("⚠️ Ambotorix error:\n" + subject + "\n" + body)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram alert to admin: {}", e.getMessage(), e);
        }
    }
}
