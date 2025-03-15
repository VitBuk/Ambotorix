package vitbuk.com.Ambotorix;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class Ambotorix implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    AmbotorixService ambotorixService;

    public Ambotorix(AmbotorixService ambotorixService) {
        this.ambotorixService = ambotorixService;
    }

    @Override
    public String getBotToken() {
        return Constants.BOT_TOKEN;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        // /[command] - for every main amborotix commands
        if (messageText.startsWith("/")) {
            switch (messageText) {
                case "/lobby" -> {
                    ambotorixService.sendLobby(chatId);
                }
                case "/leaders" -> {
                    ambotorixService.sendLeaders(chatId);
                }
            }

            // ![command] -> for every get description of the leader command
        } else if (messageText.startsWith("!")) {
            String shortName = messageText.substring(1).trim();
            // ambotorixServicecommand

            // for unknown commands
        } else {
            System.out.println("Uknown command");
            //ambotorixService unknown command method
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}
