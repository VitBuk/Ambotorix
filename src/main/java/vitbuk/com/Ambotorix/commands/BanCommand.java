package vitbuk.com.Ambotorix.commands;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import vitbuk.com.Ambotorix.commands.structure.CommandConstants;
import vitbuk.com.Ambotorix.commands.structure.DynamicCommand;
import vitbuk.com.Ambotorix.commands.structure.PlayerCommand;
import vitbuk.com.Ambotorix.services.AmbotorixService;

@Component
public class BanCommand implements DynamicCommand, PlayerCommand {
    @Override
    public String getPrefix() {
        return CommandConstants.BAN;
    }
    @Override
    public String getName() {
        return CommandConstants.BAN_NAME;
    }

    @Override
    public void execute(Update update, AmbotorixService ambotorixService) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        String userName = update.getMessage().getChat().getUserName();
        String shortName = messageText.substring(CommandConstants.BAN.length()).trim();

        ambotorixService.sendBanLeader(chatId, userName, shortName);
    }
}
