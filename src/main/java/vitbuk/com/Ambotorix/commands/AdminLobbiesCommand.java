package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.AdminCommand;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class AdminLobbiesCommand implements AdminCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/adminLobbies", "/adminLobbies", "List all active lobbies (Admin only)");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        service.sendAdminLobbies(update);
    }
}
