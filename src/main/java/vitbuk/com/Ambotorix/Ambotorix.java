package vitbuk.com.Ambotorix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import vitbuk.com.Ambotorix.commands.structure.*;
import vitbuk.com.Ambotorix.config.BotConfig;
import vitbuk.com.Ambotorix.services.AmbotorixService;

import java.util.regex.Pattern;

@Component
public class Ambotorix implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(Ambotorix.class);

    private final AmbotorixService ambotorixService;
    private final CommandFactory commandFactory;
    private final BotConfig botConfig;

    public Ambotorix(AmbotorixService ambotorixService, CommandFactory commandFactory, BotConfig botConfig) {
        this.ambotorixService = ambotorixService;
        this.commandFactory = commandFactory;
        this.botConfig = botConfig;
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            ambotorixService.makeCallbackQuery(update);
            return;
        }

        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        String messageText = message.getText();

        if (messageText == null || !messageText.startsWith("/")) return;
        String username = botConfig.getUsername().replaceFirst("^@", "");
        String cleaned = messageText.replaceAll("(?i)@" + Pattern.quote(username), "");
        message.setText(cleaned);

        Command command = getCommand(update);
        if (command == null) {
            ambotorixService.sendUnknown(update);
            return;
        }

        if (command instanceof AdminCommand) {
            if (update.getMessage().getFrom() == null
                    || !update.getMessage().getFrom().getId().equals(botConfig.getAdminId())) {
                ambotorixService.sendMessage(update, "This command is for bot admin only.");
                return;
            }
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

        if (command instanceof DynamicCommand) {
            String[] tokens = update.getMessage().getText().split("[\\s_]+", 2);
            if (tokens.length < 2 || tokens[1].isBlank()) {
                ambotorixService.sendMessage(update, "Usage: " + command.getInfo().name());
                return;
            }
        }

        command.execute(update, ambotorixService);
    }

    private Command getCommand(Update update) {
        String[] tokens = update.getMessage().getText().split("[\\s_]+");
        String commandToken = tokens[0];
        return commandFactory.getCommand(commandToken.trim());
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        log.info("Registered bot running state is: {}", botSession.isRunning());
    }
}