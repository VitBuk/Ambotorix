package vitbuk.com.Ambotorix.photochallenge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds the photo-challenge leaderboard message: fetches the sheet CSV, parses it, and renders a
 * monospace table. Errors are turned into friendly user-facing text rather than propagated.
 */
@Service
public class PhotoChallengeService {

    private static final Logger log = LoggerFactory.getLogger(PhotoChallengeService.class);

    static final String ERROR_UNREACHABLE = "Couldn't reach the scoreboard right now, try again later.";
    static final String ERROR_EMPTY = "The scoreboard looks empty or its format changed.";

    private final SheetCsvFetcher fetcher;
    private final PhotoChallengeParser parser;

    public PhotoChallengeService(SheetCsvFetcher fetcher, PhotoChallengeParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    /** The full Telegram message (HTML) for {@code /photochallenge}, or a friendly error line. */
    public String leaderboardMessage() {
        String csv;
        try {
            csv = fetcher.fetch();
        } catch (Exception e) {
            log.warn("Photo challenge sheet fetch failed: {}", e.getMessage());
            return ERROR_UNREACHABLE;
        }

        List<Standing> standings;
        try {
            standings = parser.parse(csv);
        } catch (Exception e) {
            log.warn("Photo challenge sheet parse failed: {}", e.getMessage());
            return ERROR_EMPTY;
        }

        if (standings.isEmpty()) {
            return ERROR_EMPTY;
        }
        return renderTable(standings);
    }

    /** Renders the leaderboard as a title + monospace {@code <pre>} table. Package-private for tests. */
    static String renderTable(List<Standing> standings) {
        int rankWidth = Math.max(1, Integer.toString(standings.size()).length());
        int nameWidth = standings.stream().mapToInt(s -> s.name().length()).max().orElse(6);
        nameWidth = Math.max(nameWidth, "Player".length());

        String rowFormat = "%" + rankWidth + "s  %-" + nameWidth + "s  %3s %2s %3s %2s";

        StringBuilder table = new StringBuilder();
        table.append(String.format(rowFormat, "#", "Player", "Tot", "L", "CS", "W")).append('\n');
        int rank = 1;
        for (Standing s : standings) {
            table.append(String.format(rowFormat, rank++, s.name(),
                    s.total(), s.leaders(), s.cityStates(), s.wonders())).append('\n');
        }

        return "🏆 <b>Photo Challenge</b>\n<pre>" + escape(table.toString()) + "</pre>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
