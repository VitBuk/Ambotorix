package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.services.AmbotorixService;

import java.util.Optional;

@Component
public class MapRemoveCommand implements HostCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/mapRemove",
            "/mapRemove [mapName]",
            "Removes map from mappool of the current lobby");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String[] parts = update.getMessage().getText().trim().split("\\s+", 2);
        String mapName = parts[1].trim();
        Optional<CivMap> maybeMap = CivMap.fromDisplayNameIgnoreCase(mapName);

        if (maybeMap.isEmpty()) {
            ambotorixService.sendNoSuchMap(update);
            return;
        }

        ambotorixService.sendMapRemove(update, maybeMap.get());
    }
}
