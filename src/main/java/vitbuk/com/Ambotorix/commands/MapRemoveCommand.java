package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class MapRemoveCommand implements HostCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/mapRemove",
            "/mapRemove",
            "Removes map from mappool of the current lobby (Host Command)");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText().replace("_", "");
        String mapName = messageText.substring(getInfo().prefix().length()).trim();
        CivMap civMap = CivMap.fromDisplayNameIgnoreCase(mapName).get();

        ambotorixService.sendMapRemove(update, civMap);
    }
}
