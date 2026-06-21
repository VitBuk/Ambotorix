package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.GeneralCommand;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class LobbyCommand implements GeneralCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/lobby",
            "/lobby [draft]",
            "Create game lobby (optionally pick a draft, e.g. /lobby herson)");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String[] parts = update.getMessage().getText().trim().split("\\s+", 2);
        String draftName = parts.length > 1 ? parts[1].trim() : null;
        ambotorixService.sendLobby(update, draftName);
    }
}