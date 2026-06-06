package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.HostCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class SetDraftCommand implements HostCommand, DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/setDraft", "/setDraft [open|secret]", "Set draft strategy for this lobby (Host Command)");

    @Override public CommandInfo getInfo() { return INFO; }

    @Override
    public void execute(Update update, AmbotorixService service) {
        String[] parts = update.getMessage().getText().trim().split("\\s+", 2);
        service.sendSetDraft(update, parts[1].trim().toLowerCase());
    }
}
