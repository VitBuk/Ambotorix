package vitbuk.com.Ambotorix;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.entities.Command;
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

        if (messageText.startsWith("/d_")) {
            String shortName = messageText.substring(3).trim();
            ambotorixService.sendDescription(chatId, shortName);
        } else {
            Command.fromCommandText(messageText).ifPresentOrElse(
                    command -> {
                        switch (command) {
                            case LOBBY -> ambotorixService.sendLobby(chatId);
                            case LEADERS -> ambotorixService.sendLeaders(chatId);
                           // case REGISTER -> ambotorixService.sendRegister(chatId);
                        }
                    }, () -> {ambotorixService.sendUnknown(chatId);}
            );
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}
