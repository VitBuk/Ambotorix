package vitbuk.com.Ambotorix.draft;

import vitbuk.com.Ambotorix.entities.Leader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure resolution logic for the Herson draft — no Telegram, no Spring, so it is unit-testable.
 *
 * <p>Every player submits four ranked civilizations. Two resolution rules exist:
 *
 * <ul>
 *   <li>{@link #resolve} ("herson") — a player's <em>current</em> civ is their highest available pick;
 *       a civ wanted by 2+ players at the same step is banned and they fall through to their next pick,
 *       and a clash that survives all four picks is broken by a {@link CoinFlip}.</li>
 *   <li>{@link #resolveLow} ("herson-low") — <em>any</em> civ two or more players ranked is banned
 *       outright, regardless of priority. Every surviving civ then belongs to exactly one player, so
 *       there is never a clash (and never a coin flip). The only failure mode is a player whose four
 *       picks were all banned; that is reported as {@link Unresolvable} for the caller to surface — it
 *       is rare enough not to warrant automatic recovery.</li>
 * </ul>
 *
 * <p>{@code banned} is mutated in place as bans are discovered. For {@code herson}, a coin-flip
 * suspension is resumed by calling {@link #resolve} again once the loser's re-pick is recorded in
 * {@code assigned}.
 */
public final class HersonResolver {

    private HersonResolver() {}

    public sealed interface Step permits Complete, CoinFlip, Unresolvable {}

    /** Resolution finished: every player mapped to their final, unique civ. */
    public record Complete(Map<String, Leader> assignments) implements Step {}

    /** A clash survived all four picks: flip a coin among {@code contestants} for {@code civ}. */
    public record CoinFlip(Leader civ, List<String> contestants) implements Step {}

    /** These players had all four picks banned and cannot be assigned a civ — the draft can't resolve. */
    public record Unresolvable(List<String> players) implements Step {}

    /**
     * Plain "herson" resolution.
     *
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

    /**
     * "herson-low" resolution: ban every civ two or more players ranked (at any priority), then assign
     * each player their highest surviving pick. Survivors are unique, so this never clashes. A player
     * left with no surviving pick yields {@link Unresolvable}.
     */
    public static Step resolveLow(Map<String, List<Leader>> rankedPicks,
                                  Set<Leader> banned,
                                  Map<String, Leader> assigned) {
        List<String> unassigned = rankedPicks.keySet().stream()
                .filter(u -> !assigned.containsKey(u))
                .sorted()
                .toList();

        // Ban every civ ranked by two or more players, regardless of the priority each gave it.
        Map<Leader, Integer> holders = new LinkedHashMap<>();
        for (String u : unassigned) {
            for (Leader civ : new LinkedHashSet<>(rankedPicks.get(u))) {
                holders.merge(civ, 1, Integer::sum);
            }
        }
        holders.forEach((civ, count) -> { if (count >= 2) banned.add(civ); });

        Map<String, Leader> result = new LinkedHashMap<>(assigned);
        List<String> stranded = new ArrayList<>();
        for (String u : unassigned) {
            Leader civ = firstAvailable(rankedPicks.get(u), banned, result.values(), null);
            if (civ != null) result.put(u, civ);
            else stranded.add(u);
        }

        return stranded.isEmpty() ? new Complete(result) : new Unresolvable(stranded);
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
