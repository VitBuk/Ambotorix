package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class HelpCommand implements Command {
    @Override
    public String getInfo() {
        return CommandConstants.HELP;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendHelp(update);
    }
}
