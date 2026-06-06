package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class SetPickSizeCommand implements HostCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/setPickSize", "/setPickSize [n]", "Set number of leaders per pick pool (Host Command)");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        String[] parts = update.getMessage().getText().trim().split("\\s+", 2);
        try {
            int n = Integer.parseInt(parts[1].trim());
            service.sendSetPickSize(update, n);
        } catch (NumberFormatException e) {
            service.sendMessage(update, "Invalid number: " + parts[1]);
        }
    }
}
