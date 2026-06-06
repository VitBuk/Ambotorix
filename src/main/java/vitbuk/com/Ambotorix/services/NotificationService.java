package vitbuk.com.Ambotorix.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final TelegramClient telegramClient;

    @Value("${bot.admin.id}")
    private Long adminTelegramId;

    @Value("${admin.email}")
    private String adminEmail;

    public NotificationService(JavaMailSender mailSender, TelegramClient telegramClient) {
        this.mailSender = mailSender;
        this.telegramClient = telegramClient;
    }

    public void notifyError(String subject, String body) {
        sendTelegramAlert(subject + "\n" + body);
        sendEmailAlert(subject, body);
    }

    private void sendTelegramAlert(String text) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(adminTelegramId)
                    .text("⚠️ Ambotorix error:\n" + text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram alert to admin: {}", e.getMessage(), e);
        }
    }

    private void sendEmailAlert(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("[Ambotorix] " + subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email alert: {}", e.getMessage(), e);
        }
    }
}
