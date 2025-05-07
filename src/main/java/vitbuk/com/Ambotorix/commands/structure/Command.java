package vitbuk.com.Ambotorix.commands.structure;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public interface Command {
    String getInfo();
    void execute(Update update, AmbotorixService ambotorixService);
}
