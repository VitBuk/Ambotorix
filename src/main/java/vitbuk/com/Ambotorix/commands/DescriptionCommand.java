package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class DescriptionCommand implements DynamicCommand {
    @Override
    public String getInfo() {
        return CommandConstants.DESCRIPTION;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText().replace("_", "");
        String shortName = messageText.substring(getInfo().length()).trim();

        ambotorixService.sendDescription(update, shortName);
    }
}
