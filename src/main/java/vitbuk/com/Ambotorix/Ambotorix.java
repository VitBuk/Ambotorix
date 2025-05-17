package vitbuk.com.Ambotorix;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import vitbuk.com.Ambotorix.commands.structure.*;
import vitbuk.com.Ambotorix.services.AmbotorixService;

import java.util.regex.Pattern;

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
        Message message = update.getMessage();
        String messageText = message.getText();

        if (messageText == null || !messageText.startsWith("/")) return;
        String cleaned = messageText.replaceAll("(?i)@" + Pattern.quote(Constants.BOT_USERNAME), "");
        message.setText(cleaned);

        Command command = getCommand(update);
        if (command == null) {
            ambotorixService.sendUnknown(update);
            return;
        }

        if (command instanceof HostCommand) {
            if (!ambotorixService.hasLobby(update)) {
                ambotorixService.sendNoLobby(update);
                return;
            }

            if (!ambotorixService.isHost(update)) {
                ambotorixService.sendNotAHost(update);
                return;
            }
        }

        if (command instanceof PlayerCommand) {
            if (!ambotorixService.hasLobby(update)) {
                ambotorixService.sendNoLobby(update);
                return;
            }

            if (!ambotorixService.isRegistered(update)){
                ambotorixService.sendNotAPlayer(update);
                return;
            }
        }

        System.out.println("t:" + update.getMessage().getText());
        command.execute(update, ambotorixService);
    }

    private Command getCommand(Update update) {
        String[] tokens = update.getMessage().getText().split("[\\s_]+");
        String commandToken = tokens[0];
        return commandFactory.getCommand(commandToken.trim());
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}