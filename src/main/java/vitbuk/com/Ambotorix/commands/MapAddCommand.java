package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class MapAddCommand implements HostCommand, DynamicCommand {
    @Override
    public String getPrefix() {
        return CommandConstants.ADDMAP;
    }

    @Override
    public String getName() {
        return CommandConstants.ADDMAP_NAME;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {

    }
}
