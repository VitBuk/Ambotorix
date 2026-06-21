package vitbuk.com.Ambotorix.draft;

import org.junit.jupiter.api.Test;
import vitbuk.com.Ambotorix.entities.Leader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HersonResolverTest {

    private static Leader civ(String name) { return new Leader(name, name.toLowerCase(), "", ""); }

    private static final Leader A = civ("A");
    private static final Leader B = civ("B");
    private static final Leader C = civ("C");
    private static final Leader D = civ("D");
    private static final Leader E = civ("E");

    private HersonResolver.Step resolve(Map<String, List<Leader>> picks) {
        return HersonResolver.resolve(picks, new HashSet<>(), new LinkedHashMap<>());
    }

    @Test
    void noClash_assignsEveryonesTopPick() {
        Map<String, List<Leader>> picks = new LinkedHashMap<>();
        picks.put("alice", List.of(A, B, C, D));
        picks.put("bob", List.of(E, D, C, B));

        HersonResolver.Step step = resolve(picks);

        HersonResolver.Complete complete = assertInstanceOf(HersonResolver.Complete.class, step);
        assertEquals(A, complete.assignments().get("alice"));
        assertEquals(E, complete.assignments().get("bob"));
    }

    @Test
    void clash_bansContestedCivAndFallsThrough() {
        Map<String, List<Leader>> picks = new LinkedHashMap<>();
        picks.put("alice", List.of(A, B, C, D));
        picks.put("bob", List.of(A, E, C, D));

        Set<Leader> banned = new HashSet<>();
        HersonResolver.Step step = HersonResolver.resolve(picks, banned, new LinkedHashMap<>());

        HersonResolver.Complete complete = assertInstanceOf(HersonResolver.Complete.class, step);
        assertEquals(B, complete.assignments().get("alice"));
        assertEquals(E, complete.assignments().get("bob"));
        assertTrue(banned.contains(A), "the contested civ should be banned");
    }

    @Test
    void repeatedClash_bansEachContestedCivUntilUnique() {
        Map<String, List<Leader>> picks = new LinkedHashMap<>();
        picks.put("alice", List.of(A, B, C, D));
        picks.put("bob", List.of(A, B, E, D));

        HersonResolver.Step step = resolve(picks);

        HersonResolver.Complete complete = assertInstanceOf(HersonResolver.Complete.class, step);
        assertEquals(C, complete.assignments().get("alice"));
        assertEquals(E, complete.assignments().get("bob"));
    }

    @Test
    void clashSurvivingAllFourPicks_requestsCoinFlip() {
        Map<String, List<Leader>> picks = new LinkedHashMap<>();
        picks.put("alice", List.of(A, B, C, D));
        picks.put("bob", List.of(A, B, C, D));

        HersonResolver.Step step = resolve(picks);

        HersonResolver.CoinFlip flip = assertInstanceOf(HersonResolver.CoinFlip.class, step);
        assertEquals(D, flip.civ(), "the last surviving shared pick is the coin-flip civ");
        assertTrue(flip.contestants().containsAll(List.of("alice", "bob")));
    }

    @Test
    void preBannedCivs_areSkipped() {
        // A host ban is fed in as an already-banned civ; the player falls through past it.
        Map<String, List<Leader>> picks = new LinkedHashMap<>();
        picks.put("alice", List.of(A, B, C, D));

        Set<Leader> banned = new HashSet<>(Set.of(A));
        HersonResolver.Step step = HersonResolver.resolve(picks, banned, new LinkedHashMap<>());

        HersonResolver.Complete complete = assertInstanceOf(HersonResolver.Complete.class, step);
        assertEquals(B, complete.assignments().get("alice"));
    }

    @Test
    void alreadyAssignedCivs_areTreatedAsTaken() {
        Map<String, List<Leader>> picks = new LinkedHashMap<>();
        picks.put("alice", List.of(A, B, C, D)); // A is taken below, so alice's top available is B
        picks.put("carol", List.of(B, D, E, A)); // clashes with alice on B

        Set<Leader> banned = new HashSet<>();
        Map<String, Leader> assigned = new HashMap<>();
        assigned.put("bob", A); // bob already holds A (e.g. a coin-flip winner / re-pick)

        HersonResolver.Step step = HersonResolver.resolve(picks, banned, assigned);

        // B is contested (alice via A-taken, carol top) -> banned. alice: A taken, B banned -> C.
        // carol: B banned -> D.
        HersonResolver.Complete complete = assertInstanceOf(HersonResolver.Complete.class, step);
        assertEquals(A, complete.assignments().get("bob"));
        assertEquals(C, complete.assignments().get("alice"));
        assertEquals(D, complete.assignments().get("carol"));
        assertTrue(banned.contains(B));
    }
}
