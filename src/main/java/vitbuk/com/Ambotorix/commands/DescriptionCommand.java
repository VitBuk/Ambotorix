package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandInfo;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class DescriptionCommand implements DynamicCommand {
    private static final CommandInfo INFO = new CommandInfo(
            "/d",
            "/d [shortName]",
            "Show description of a leader");
    @Override
    public CommandInfo getInfo() {
        return INFO;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText().replace("_", "");
        String shortName = messageText.substring(getInfo().prefix().length()).trim();

        ambotorixService.sendDescription(update, shortName);
    }
}
