package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class MaplistCommand implements Command {
    @Override
    public String getPrefix() {
        return CommandConstants.MAPLIST;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {

    }
}
