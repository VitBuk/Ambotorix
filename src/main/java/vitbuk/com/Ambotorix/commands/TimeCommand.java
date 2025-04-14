package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class TimeCommand implements Command{
    @Override
    public String getCommandName() {
        return CommandNames.TIME;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {

    }
}
