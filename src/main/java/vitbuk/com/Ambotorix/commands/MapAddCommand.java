package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandFactory;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.services.AmbotorixService;

import java.util.Optional;

@Component
public class MapAddCommand implements HostCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/mapAdd",
            "/mapAdd [map Name]",
            "Add map to a mappool of the lobby (Host command)");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String text = update.getMessage().getText().trim();
        String[] parts = text.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            ambotorixService.sendNoSuchMap(update);
            return;
        }

        String mapName = parts[1].trim();
        Optional<CivMap> maybeMap = CivMap.fromDisplayNameIgnoreCase(mapName);

        if (maybeMap.isEmpty()) {
            ambotorixService.sendNoSuchMap(update);
            return;
        }

        ambotorixService.sendMapAdd(update, maybeMap.get());
    }
}
