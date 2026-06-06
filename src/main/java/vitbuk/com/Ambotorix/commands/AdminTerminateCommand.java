package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.AdminCommand;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class AdminTerminateCommand implements AdminCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/adminTerminate", "/adminTerminate [chatId]", "Force-terminate a lobby by chatId (Admin only)");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        String[] parts = update.getMessage().getText().trim().split("\\s+", 2);
        try {
            Long targetChatId = Long.parseLong(parts[1].trim());
            service.sendAdminTerminate(update, targetChatId);
        } catch (NumberFormatException e) {
            service.sendMessage(update, "Invalid chatId: " + parts[1]);
        }
    }
}
