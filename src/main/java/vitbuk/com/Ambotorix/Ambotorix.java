package vitbuk.com.Ambotorix;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.*;
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
        if (messageText == null || !messageText.startsWith(CommandConstants.PREFIX)) return;

        // we look at the first part of the message before first whitespace or underscore
        String[] parts = messageText.split("[\\s_]+");
        Command command = commandFactory.getCommand(parts[0].trim());

        if (command == null) {
            ambotorixService.sendUnknown(update);
            return;
        }

        if (command instanceof HostCommand) {
            if (!ambotorixService.isHost(update)) {
                ambotorixService.sendNotAHost(update);
                return;
            }
        }

        if (command instanceof PlayerCommand) {
            if (!ambotorixService.isRegistered(update)){
                ambotorixService.sendNotAPlayer(update);
                return;
            }
        }

        command.execute(update, ambotorixService);
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}