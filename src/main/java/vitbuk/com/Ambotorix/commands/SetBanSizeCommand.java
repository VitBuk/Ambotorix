package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class SetBanSizeCommand implements HostCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/setBanSize", "/setBanSize [n]", "Set number of bans per player");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        String[] parts = update.getMessage().getText().trim().split("\\s+", 2);
        try {
            int n = Integer.parseInt(parts[1].trim());
            service.sendSetBanSize(update, n);
        } catch (NumberFormatException e) {
            service.sendMessage(update, "Invalid number: " + parts[1]);
        }
    }
}
