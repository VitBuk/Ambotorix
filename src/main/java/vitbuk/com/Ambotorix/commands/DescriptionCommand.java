package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class DescriptionCommand implements Command{
    @Override
    public String getCommandName() {
        return CommandNames.DESCRIPTION;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        String messageText = update.getMessage().getText();
        String shortName = messageText.substring(getCommandName().length());

        ambotorixService.sendDescription(update.getMessage().getChatId(), shortName);
    }
}
