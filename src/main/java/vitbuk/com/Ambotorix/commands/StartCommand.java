package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class StartCommand implements HostCommand {
    @Override
    public String getPrefix() {
        return CommandConstants.START;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {

    }
}
