package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class BanCommand implements Command {
    @Override
    public String getPrefix() {
        return CommandConstants.BAN;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        String userName = update.getMessage().getChat().getUserName();
        String shortName = messageText.substring(CommandConstants.BAN.length());

        ambotorixService.sendBanLeader(chatId, userName, shortName);
    }
}
