package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.PlayerCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class PickCommand implements PlayerCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/pick", "/pick [shortName]", "Pick a leader in secret draft (Player Command)");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        String[] parts = update.getMessage().getText().trim().split("[\\s_]+", 2);
        service.sendPick(update, update.getMessage().getChatId(), parts[1].trim());
    }
}
