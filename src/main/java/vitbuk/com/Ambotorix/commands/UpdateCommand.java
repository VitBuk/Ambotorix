package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class UpdateCommand implements Command {
    private static final CommandInfo INFO = new CommandInfo(
            "/update", "/update",
            "Check for BBG updates and refresh leader data if a new version is available");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        service.sendUpdate(update);
    }
}
