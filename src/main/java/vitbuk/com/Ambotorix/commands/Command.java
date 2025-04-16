package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public interface Command {
    String getPrefix();
    String getCommandName();
    void execute(Update update, AmbotorixService ambotorixService);
}
