package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.PlayerCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class BanButtonsCommand implements PlayerCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/banButtons",
            "/banButtons",
            "Receive ban buttons in DM for all available leaders");

    @Override
    public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendBanButtons(update);
    }
}
