package vitbuk.com.Ambotorix.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import vitbuk.com.Ambotorix.commands.DescriptionCommand;
import vitbuk.com.Ambotorix.commands.MapAddCommand;
import vitbuk.com.Ambotorix.commands.MaplistCommand;
import vitbuk.com.Ambotorix.commands.structure.Command;
import vitbuk.com.Ambotorix.commands.structure.CommandFactory;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.entities.Leader;

import java.util.ArrayList;
import java.util.List;

@Service
public class MarkupService {

    private final CommandFactory commandFactory;

    @Autowired
    public MarkupService(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public InlineKeyboardMarkup leadersMarkup(List<Leader> leaders) {
        String dPrefix = commandFactory.infoOf(DescriptionCommand.class).prefix();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (Leader l : leaders) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(l.getFullName())
                    .callbackData(dPrefix + " " + l.getShortName())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup maplistMarkup(List<CivMap> maps) {
        String mPrefix = commandFactory.infoOf(MapAddCommand.class).prefix();

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (CivMap m : maps) {
            System.out.println("mPrefix + m.toString(): " + mPrefix + m.toString());
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(m.name())
                    .callbackData(mPrefix + " " + m.toString())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}
