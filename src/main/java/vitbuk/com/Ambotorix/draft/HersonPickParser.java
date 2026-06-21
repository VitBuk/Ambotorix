package vitbuk.com.Ambotorix.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a player's free-text Herson submission — e.g. {@code "1. Gandhi 2. Lincoln 3. Saladin
 * 4. Trajan"} — into the raw civ strings in rank order. Whitespace and the separator after the rank
 * number ({@code .)} :} or nothing) are ignored; civ names never contain digits, so the rank digits
 * delimit the entries cleanly. The matched civ strings are returned untouched (not resolved to
 * leaders) — fuzzy matching happens downstream.
 */
public final class HersonPickParser {

    private HersonPickParser() {}

    // A rank marker (1-4, optionally followed by . ) : - ) then the civ text up to the next marker.
    private static final Pattern ENTRY = Pattern.compile(
            "([1-4])\\s*[.)\\-:]?\\s*([^0-9]+?)\\s*(?=(?:[1-4]\\s*[.)\\-:]?\\s*[^0-9])|$)");

    /**
     * @return the four civ strings in rank order, or an empty list if the message is not a clean
     *         "1..4" submission (wrong count, missing/duplicate ranks, or unparseable).
     */
    public static List<String> parse(String text) {
        if (text == null) return List.of();
        String[] slots = new String[4];
        Matcher m = ENTRY.matcher(text);
        while (m.find()) {
            int rank = Integer.parseInt(m.group(1));
            String name = m.group(2).trim();
            if (name.isEmpty()) return List.of();
            if (slots[rank - 1] != null) return List.of(); // duplicate rank number
            slots[rank - 1] = name;
        }
        List<String> result = new ArrayList<>(4);
        for (String s : slots) {
            if (s == null) return List.of(); // a rank 1..4 was missing
            result.add(s);
        }
        return result;
    }
}
