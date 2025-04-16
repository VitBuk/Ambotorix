package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class DescriptionCommand implements Command {
    @Override
    public String getPrefix() {
        return CommandConstants.DESCRIPTION;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText();
        String shortName = messageText.substring(getPrefix().length());

        ambotorixService.sendDescription(update.getMessage().getChatId(), shortName);
    }
}
