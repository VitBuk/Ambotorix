package vitbuk.com.Ambotorix.photochallenge;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the leaderboard CSV into {@link Standing}s. Columns are located by header name
 * (case-insensitive) so reordering them in the sheet does not break parsing. Row order is
 * preserved — the sheet tab is already sorted by total.
 */
@Component
public class PhotoChallengeParser {

    private static final String COL_PLAYER = "player";
    private static final String COL_TOTAL = "total";
    private static final String COL_LEADERS = "leaders";
    private static final String COL_CITY_STATES = "city-states";
    private static final String COL_WONDERS = "wonders";

    /**
     * @throws IllegalArgumentException if the CSV is empty or is missing a required column header.
     */
    public List<Standing> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("Empty CSV");
        }
        List<String> lines = csv.lines().filter(l -> !l.isBlank()).toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("No rows in CSV");
        }

        Map<String, Integer> col = headerIndex(parseLine(lines.get(0)));
        for (String required : List.of(COL_PLAYER, COL_TOTAL, COL_LEADERS, COL_CITY_STATES, COL_WONDERS)) {
            if (!col.containsKey(required)) {
                throw new IllegalArgumentException("Missing column: " + required);
            }
        }

        List<Standing> standings = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> fields = parseLine(lines.get(i));
            String name = at(fields, col.get(COL_PLAYER)).trim();
            if (name.isEmpty()) continue; // skip blank/trailing rows
            standings.add(new Standing(
                    name,
                    toInt(at(fields, col.get(COL_TOTAL))),
                    toInt(at(fields, col.get(COL_LEADERS))),
                    toInt(at(fields, col.get(COL_CITY_STATES))),
                    toInt(at(fields, col.get(COL_WONDERS)))));
        }
        return standings;
    }

    private static Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            index.put(header.get(i).trim().toLowerCase(), i);
        }
        return index;
    }

    private static String at(List<String> fields, int i) {
        return i >= 0 && i < fields.size() ? fields.get(i) : "";
    }

    private static int toInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Minimal CSV line parser: handles quoted fields and escaped ("") quotes. */
    private static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
