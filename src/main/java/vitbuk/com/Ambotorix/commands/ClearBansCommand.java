package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class ClearBansCommand implements HostCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/clearBans",
            "/clearBans",
            "Clear all bans so players can redo their bans (Host command)");

    @Override
    public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendClearBans(update);
    }
}
