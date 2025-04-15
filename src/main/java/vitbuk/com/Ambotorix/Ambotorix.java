package vitbuk.com.Ambotorix;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.Command;
import vitbuk.com.Ambotorix.commands.CommandFactory;
import vitbuk.com.Ambotorix.commands.CommandNames;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class Ambotorix implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final AmbotorixService ambotorixService;
    private final CommandFactory commandFactory;

    public Ambotorix(AmbotorixService ambotorixService, CommandFactory commandFactory) {
        this.ambotorixService = ambotorixService;
        this.commandFactory = commandFactory;
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

        if (messageText.startsWith(CommandNames.PREFIX)) {
            Command command;

            if (messageText.startsWith(CommandNames.DESCRIPTION)){
                command = commandFactory.getCommand(CommandNames.DESCRIPTION);
            } else if (messageText.startsWith(CommandNames.BAN)){
                command = commandFactory.getCommand(CommandNames.BAN);
            } else {
                command = commandFactory.getCommand(messageText);
            }

            if (command == null ) {
                ambotorixService.sendUnknown(update.getMessage().getChatId());
            } else {
                command.execute(update, ambotorixService);
            }
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}