package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class LeadersCommand implements Command {
    @Override
    public String getCommandName() {
        return CommandNames.LEADERS;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendLeaders(update.getMessage().getChatId());
    }
}
