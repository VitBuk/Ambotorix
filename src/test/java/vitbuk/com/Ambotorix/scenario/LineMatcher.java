package vitbuk.com.Ambotorix.scenario;

import vitbuk.com.Ambotorix.entities.CivMap;
import vitbuk.com.Ambotorix.harness.HtmlNormalizer;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * One matching engine for both message text and button labels (SCENARIO_TESTING_PLAN.md §7). A
 * pattern is an exact whole-string match after HTML normalization, with {@code *} (any run) and
 * {@code {player}}/{@code {leader}}/{@code {map}} set placeholders the only wildcards. Placeholders
 * that come from runtime data (players, leaders) are supplied per scenario via {@link MatchContext};
 * {@code {map}} comes from the {@link CivMap} enum.
 */
public final class LineMatcher {

    private LineMatcher() {}

    /** Placeholder vocabularies resolved from runtime data for a given scenario. */
    public record MatchContext(Collection<String> players, Collection<String> leaders) {}

    /** Does {@code text} satisfy the expected {@code pattern}? */
    public static boolean matches(String pattern, String text, MatchContext ctx) {
        String normalized = HtmlNormalizer.normalize(text);
        return Pattern.compile(toRegex(pattern, ctx)).matcher(normalized).matches();
    }

    private static String toRegex(String pattern, MatchContext ctx) {
        String collapsed = pattern.replaceAll("\\s+", " ").trim();
        StringBuilder re = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < collapsed.length()) {
            char c = collapsed.charAt(i);
            if (c == '*') {
                flush(literal, re);
                re.append(".*");
                i++;
            } else if (c == '{') {
                int end = collapsed.indexOf('}', i);
                if (end < 0) { literal.append(c); i++; continue; }
                flush(literal, re);
                re.append(placeholder(collapsed.substring(i + 1, end).trim(), ctx));
                i = end + 1;
            } else {
                literal.append(c);
                i++;
            }
        }
        flush(literal, re);
        return re.toString();
    }

    private static void flush(StringBuilder literal, StringBuilder re) {
        if (literal.length() > 0) {
            re.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }

    private static String placeholder(String kind, MatchContext ctx) {
        return switch (kind.toLowerCase()) {
            case "player" -> alternation(ctx == null ? null : ctx.players());
            case "leader" -> alternation(ctx == null ? null : ctx.leaders());
            case "map" -> alternation(Arrays.stream(CivMap.values()).map(CivMap::toString).collect(Collectors.toList()));
            default -> ".+?"; // unknown kind: any non-empty token(s)
        };
    }

    private static String alternation(Collection<String> options) {
        if (options == null || options.isEmpty()) return ".+?";
        return "(?:" + options.stream()
                .map(HtmlNormalizer::normalize)
                .map(Pattern::quote)
                .collect(Collectors.joining("|")) + ")";
    }
}
