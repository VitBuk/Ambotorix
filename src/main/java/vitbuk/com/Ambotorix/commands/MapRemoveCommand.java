package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class MapRemoveCommand implements HostCommand, DynamicCommand {
    @Override
    public String getPrefix() {
        return CommandConstants.MAPREMOVE;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText().replace("_", "");
        String mapName = messageText.substring(getPrefix().length()).trim();
        CivMap civMap = CivMap.fromDisplayNameIgnoreCase(mapName).get();

        ambotorixService.sendMapRemove(update, civMap);
    }

    @Override
    public String getName() {
        return CommandConstants.MAPREMOVE;
    }
}
