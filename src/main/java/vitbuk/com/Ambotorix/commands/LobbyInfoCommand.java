package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class LobbyInfoCommand implements Command {
    private static final CommandInfo INFO = new CommandInfo(
            "/lobbyInfo", "/lobbyInfo", "Show current lobby configuration and player list");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        service.sendLobbyInfo(update);
    }
}
