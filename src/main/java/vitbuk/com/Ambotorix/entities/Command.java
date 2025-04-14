package vitbuk.com.Ambotorix.entities;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public interface Command {
    String getCommandName();
    void execute(Update update, AmbotorixService ambotorixService);
}
