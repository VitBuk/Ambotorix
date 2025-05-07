package vitbuk.com.Ambotorix.commands.structure;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public interface Command {
    CommandInfo getInfo();
    void execute(Update update, AmbotorixService ambotorixService);
}
