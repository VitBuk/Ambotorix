package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class LeadersCommand implements Command {
    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public String getCommandName() {
        return CommandNames.LEADERS;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendLeaders(update.getMessage().getChatId());
    }
}
