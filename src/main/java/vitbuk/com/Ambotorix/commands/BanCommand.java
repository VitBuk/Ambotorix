package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.PlayerCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class BanCommand implements PlayerCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/ban",
            "/ban [shortName]",
            "Ban leader for current lobby");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        // Everything after "/ban" (or "/ban_") is a free-form query; the matcher handles formatting,
        // typos and partial names, so we only strip the leading separator here.
        String text = update.getMessage().getText().trim();
        String query = text.substring(getInfo().prefix().length()).replaceFirst("^[\\s_]+", "").trim();

        ambotorixService.sendSmartBan(update, query);
    }
}
