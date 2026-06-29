package vitbuk.com.Ambotorix.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import vitbuk.com.Ambotorix.commands.BanCommand;
import vitbuk.com.Ambotorix.commands.DescriptionCommand;
import vitbuk.com.Ambotorix.commands.MapAddCommand;
import vitbuk.com.Ambotorix.commands.MapRemoveCommand;
import vitbuk.com.Ambotorix.commands.structure.CommandFactory;
import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.entities.Leader;

import java.util.ArrayList;
import java.util.List;

@Service
public class MarkupService {

    private static final Logger log = LoggerFactory.getLogger(MarkupService.class);
    private final CommandFactory commandFactory;

    @Autowired
    public MarkupService(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    // Number of leader buttons per row in the grid layout. Buttons use the compact
    // display name (getDisplayName), so 3 columns fit without heavy wrapping.
    private static final int LEADERS_GRID_COLUMNS = 3;

    // One leader per row, labelled with the full name. Used where vertical space is
    // not a concern (e.g. the per-player draft DM).
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
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup leadersGridMarkup(List<Leader> leaders) {
        return leadersGridMarkup(leaders, LEADERS_GRID_COLUMNS);
    }

    // Compact grid using the short display name, packed into the given number of columns.
    public InlineKeyboardMarkup leadersGridMarkup(List<Leader> leaders, int columns) {
        String dPrefix = commandFactory.infoOf(DescriptionCommand.class).prefix();
        int cols = Math.max(1, columns);
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Leader l : leaders) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(l.getDisplayName())
                    .callbackData(dPrefix + " " + l.getShortName())
                    .build();
            row.add(btn);
            if (row.size() == cols) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup pickMarkup(List<Leader> leaders, Long groupChatId) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Leader l : leaders) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text("Pick " + l.getFullName())
                    .callbackData("/pick " + groupChatId + " " + l.getShortName())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    // Priority-ranking grid for interactive Herson picks. Already-ranked leaders show their rank number.
    // SUBMIT and RESET action buttons appear below the leader grid.
    public InlineKeyboardMarkup hersonPickMarkup(List<Leader> leaders, List<String> priorityPicks, Long groupChatId) {
        int cols = LEADERS_GRID_COLUMNS;
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (Leader l : leaders) {
            int rank = priorityPicks.indexOf(l.getShortName());
            String label = rank >= 0 ? l.getDisplayName() + " (" + (rank + 1) + ")" : l.getDisplayName();
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData("/hpick " + groupChatId + " " + l.getShortName())
                    .build();
            row.add(btn);
            if (row.size() == cols) {
                rows.add(row);
                row = new InlineKeyboardRow();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        InlineKeyboardRow actionRow = new InlineKeyboardRow();
        actionRow.add(InlineKeyboardButton.builder().text("✅ SUBMIT").callbackData("/hsubmit " + groupChatId).build());
        actionRow.add(InlineKeyboardButton.builder().text("🔄 RESET").callbackData("/hreset " + groupChatId).build());
        rows.add(actionRow);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /** Confirm / re-enter buttons for a Herson submission whose civ names were fuzzily auto-corrected. */
    public InlineKeyboardMarkup hersonConfirmMarkup(Long groupChatId) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(InlineKeyboardButton.builder()
                .text("✅ Confirm")
                .callbackData("/hconfirm " + groupChatId)
                .build());
        row.add(InlineKeyboardButton.builder()
                .text("✏️ Re-enter")
                .callbackData("/hredo " + groupChatId)
                .build());
        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }

    public InlineKeyboardMarkup maplistMarkup(List<CivMap> maps, Long groupChatId) {
        String mPrefix = commandFactory.infoOf(MapAddCommand.class).prefix();

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (CivMap m : maps) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(m.name())
                    .callbackData(mPrefix + " " + groupChatId + " " + m.toString())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup banButtonsMarkup(List<Leader> leaders, Long groupChatId) {
        String banPrefix = commandFactory.infoOf(BanCommand.class).prefix();

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Leader l : leaders) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(l.getFullName())
                    .callbackData(banPrefix + " " + groupChatId + " " + l.getShortName())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    public InlineKeyboardMarkup mapRemoveMarkup(List<CivMap> maps, Long groupChatId) {
        String rPrefix = commandFactory.infoOf(MapRemoveCommand.class).prefix();

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (CivMap m : maps) {
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text("❌ " + m.toString())
                    .callbackData(rPrefix + " " + groupChatId + " " + m.toString())
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
