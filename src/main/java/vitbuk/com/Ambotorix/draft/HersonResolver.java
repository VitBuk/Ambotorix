package vitbuk.com.Ambotorix.draft;

import vitbuk.com.Ambotorix.entities.Leader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure resolution logic for the Herson draft — no Telegram, no Spring, so it is unit-testable.
 *
 * <p>Every player submits four ranked civilizations. A player's <em>current</em> civ is the highest
 * ranked one still available (not banned, not already taken by someone else). The algorithm advances
 * in steps:
 *
 * <ol>
 *   <li>If no two players currently want the same civ, everyone is assigned their current civ —
 *       {@link Complete}.</li>
 *   <li>If a contested civ can be banned without leaving any contestant empty-handed, it is banned
 *       (the contestants fall through to their next pick) and the loop repeats.</li>
 *   <li>If the only remaining clashes are ones where banning would strand a contestant (i.e. it is
 *       their last available pick), the caller must break the tie with a coin flip —
 *       {@link CoinFlip}.</li>
 * </ol>
 *
 * <p>{@code banned} is mutated in place as safe bans are discovered, so a coin-flip suspension can be
 * resumed by simply calling {@link #resolve} again once the loser's re-pick has been recorded in
 * {@code assigned}.
 */
public final class HersonResolver {

    private HersonResolver() {}

    public sealed interface Step permits Complete, CoinFlip {}

    /** Resolution finished: every player mapped to their final, unique civ. */
    public record Complete(Map<String, Leader> assignments) implements Step {}

    /** A clash survived all four picks: flip a coin among {@code contestants} for {@code civ}. */
    public record CoinFlip(Leader civ, List<String> contestants) implements Step {}

    /**
     * @param rankedPicks username -> their four ranked picks (priority order)
     * @param banned      civs already removed from the pool; mutated as more are banned
     * @param assigned    username -> final civ for players already resolved (coin-flip winners /
     *                    re-pickers); never reused for other players
     */
    public static Step resolve(Map<String, List<Leader>> rankedPicks,
                               Set<Leader> banned,
                               Map<String, Leader> assigned) {
        while (true) {
            List<String> unresolved = rankedPicks.keySet().stream()
                    .filter(u -> !assigned.containsKey(u))
                    .sorted()
                    .toList();
            Collection<Leader> taken = assigned.values();

            // Each unresolved player's current top-available civ.
            Map<String, Leader> current = new LinkedHashMap<>();
            for (String u : unresolved) {
                Leader c = firstAvailable(rankedPicks.get(u), banned, taken, null);
                if (c != null) current.put(u, c);
            }

            // Group players by the civ they currently want.
            Map<Leader, List<String>> byCiv = new LinkedHashMap<>();
            for (Map.Entry<String, Leader> e : current.entrySet()) {
                byCiv.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }

            List<Map.Entry<Leader, List<String>>> clashes = byCiv.entrySet().stream()
                    .filter(e -> e.getValue().size() >= 2)
                    .sorted(Comparator.comparing(e -> e.getKey().getFullName()))
                    .toList();

            if (clashes.isEmpty()) {
                Map<String, Leader> result = new LinkedHashMap<>(assigned);
                result.putAll(current);
                return new Complete(result);
            }

            // Prefer banning a clash that strands nobody; that lets contestants fall through.
            Map.Entry<Leader, List<String>> safe = null;
            for (Map.Entry<Leader, List<String>> clash : clashes) {
                if (isSafeToBan(clash.getKey(), clash.getValue(), rankedPicks, banned, taken)) {
                    safe = clash;
                    break;
                }
            }
            if (safe != null) {
                banned.add(safe.getKey());
                continue;
            }

            // Every remaining clash is terminal — resolve the smallest-named one by coin flip.
            Map.Entry<Leader, List<String>> terminal = clashes.get(0);
            return new CoinFlip(terminal.getKey(), List.copyOf(terminal.getValue()));
        }
    }

    /** Banning {@code civ} is safe only if every contestant still has another available pick. */
    private static boolean isSafeToBan(Leader civ, List<String> contestants,
                                       Map<String, List<Leader>> rankedPicks,
                                       Set<Leader> banned, Collection<Leader> taken) {
        for (String u : contestants) {
            if (firstAvailable(rankedPicks.get(u), banned, taken, civ) == null) return false;
        }
        return true;
    }

    /** First ranked pick that is not banned, not taken by someone else, and not {@code extraExcluded}. */
    private static Leader firstAvailable(List<Leader> picks, Set<Leader> banned,
                                         Collection<Leader> taken, Leader extraExcluded) {
        if (picks == null) return null;
        for (Leader l : picks) {
            if (banned.contains(l)) continue;
            if (taken.contains(l)) continue;
            if (l.equals(extraExcluded)) continue;
            return l;
        }
        return null;
    }
}
