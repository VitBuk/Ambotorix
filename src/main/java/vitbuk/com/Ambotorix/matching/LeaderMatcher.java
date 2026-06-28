package vitbuk.com.Ambotorix.matching;

import org.springframework.stereotype.Component;
import vitbuk.com.Ambotorix.entities.Leader;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves a loose, human-typed ban query (e.g. {@code "alex"}, {@code "teresa"},
 * {@code "roosevelt"}) to a leader, without ever silently banning the wrong one.
 *
 * <p>Matching is tiered — exact shortName → exact name-word → prefix → fuzzy — and the first tier
 * that matches any leader wins, so an exact shortName always beats a name-word collision, prefix or
 * fuzzy match. Within the winning tier, a single distinct leader yields {@link MatchResult.Unique};
 * several yield {@link MatchResult.Ambiguous} (the caller shows buttons). Nothing matched yields
 * {@link MatchResult.None}.
 */
@Component
public class LeaderMatcher {

    /** The outcome of resolving a query against the leader list. */
    public sealed interface MatchResult {
        /** Exactly one confident match — ban it. */
        record Unique(Leader leader) implements MatchResult {}
        /** Several plausible matches — let the user pick. */
        record Ambiguous(List<Leader> leaders) implements MatchResult {}
        /** Nothing matched. */
        record None() implements MatchResult {}
    }

    public MatchResult match(String rawQuery, List<Leader> leaders) {
        String q = normalize(rawQuery);
        if (q.length() < 2) return new MatchResult.None();

        // Tier 1 — exact shortName: a perfect shortName match is unambiguous and always wins, even
        // when that word also appears in another leader's name (e.g. "nzinga" is the shortName of
        // Mvemba a Nzinga but also a name-word of Nzinga Mbande). Never ask the user to disambiguate
        // a query that exactly equals a shortName.
        List<Leader> shortNameExact = leaders.stream()
                .filter(l -> shortNameNorm(l).equals(q))
                .toList();
        if (!shortNameExact.isEmpty()) return resolve(shortNameExact);

        // Tier 2 — exact name-word: query equals a full-name token (or a shortName part).
        List<Leader> exact = leaders.stream()
                .filter(l -> tokens(l).contains(q))
                .toList();
        if (!exact.isEmpty()) return resolve(exact);

        // Tier 3 — prefix: a token starts with the query (≥3 chars); multi-word queries also
        // match as a substring of the joined full name (handles "bull moose").
        if (q.length() >= 3) {
            boolean multiWord = q.contains(" ");
            List<Leader> prefix = leaders.stream()
                    .filter(l -> tokens(l).stream().anyMatch(t -> t.startsWith(q))
                            || (multiWord && nameString(l).contains(q)))
                    .toList();
            if (!prefix.isEmpty()) return resolve(prefix);
        }

        // Tier 4 — fuzzy: a token is within a length-scaled edit distance of the query (≥4 chars).
        if (q.length() >= 4) {
            List<Leader> fuzzy = leaders.stream()
                    .filter(l -> tokens(l).stream().anyMatch(t -> withinTypoThreshold(q, t)))
                    .toList();
            if (!fuzzy.isEmpty()) return resolve(fuzzy);
        }

        return new MatchResult.None();
    }

    private MatchResult resolve(List<Leader> matches) {
        List<Leader> distinct = matches.stream()
                .distinct()
                .sorted(Comparator.comparing(Leader::getFullName))
                .toList();
        return distinct.size() == 1
                ? new MatchResult.Unique(distinct.get(0))
                : new MatchResult.Ambiguous(distinct);
    }

    // ---- token building ----

    /** Every searchable token of a leader: full-name words (incl. civ word) + shortName parts. */
    private Set<String> tokens(Leader leader) {
        Set<String> tokens = new HashSet<>();
        addWords(tokens, nameString(leader));
        addWords(tokens, shortNameNorm(leader));
        return tokens;
    }

    private void addWords(Set<String> into, String normalized) {
        for (String w : normalized.split(" ")) {
            if (!w.isBlank()) into.add(w);
        }
    }

    private String nameString(Leader leader) { return normalize(leader.getFullName()); }

    private String shortNameNorm(Leader leader) { return normalize(leader.getShortName()); }

    /** Lowercase, strip diacritics, reduce any non-alphanumeric run to a single space, trim. */
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        return n.trim().replaceAll("\\s+", " ");
    }

    // ---- fuzzy matching ----

    private boolean withinTypoThreshold(String query, String token) {
        if (token.length() < 3) return false; // tiny tokens fuzz-match too easily
        int len = Math.max(query.length(), token.length());
        int threshold = len <= 5 ? 1 : 2;
        return osaDistance(query, token) <= threshold;
    }

    /** Optimal string alignment distance (Levenshtein + adjacent transpositions). */
    private static int osaDistance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[n][m];
    }
}
