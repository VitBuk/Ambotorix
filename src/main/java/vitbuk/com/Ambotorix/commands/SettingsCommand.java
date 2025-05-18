package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class SettingsCommand implements Command {
    private static final CommandInfo INFO = new CommandInfo(
            "/settings",
            "/settings",
            "Shows recommended settings for multiplayer Civ6");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendSettings(update);
    }
}
