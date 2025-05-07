package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class HelpCommand implements Command {
    private static final CommandInfo INFO = new CommandInfo(
            "/help",
            "/help",
            "Show list of commands");
    @Override
    public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendHelp(update);
    }
}
