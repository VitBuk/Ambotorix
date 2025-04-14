package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class LobbyCommand implements Command{
    private static final String commandName =  "/lobby";
    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendLobby(update.getMessage().getChatId(), update.getMessage().getChat().getUserName());
    }
}