package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class TimeCommand implements Command{
    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public String getCommandName() {
        return CommandNames.TIME;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendTime(update.getMessage().getChatId());
    }
}
