package vitbuk.com.Ambotorix.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.services.AmbotorixService;

public class RegisterCommand implements Command {

    @Override
    public String getCommandName() {
        return CommandNames.REGISTER;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        ambotorixService.sendRegister(update.getMessage().getChatId(), update.getMessage().getChat().getUserName());
    }
}
