package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.PlayerCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class BanCommand implements DynamicCommand, PlayerCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/ban",
            "/ban [shortName]",
            "Ban leader");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText().replace("_", "");
        String shortName = messageText.substring(getInfo().prefix().length()).trim();

        ambotorixService.sendBanLeader(update, shortName);
    }
}
