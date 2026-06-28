package vitbuk.com.Ambotorix.matching;

import org.junit.jupiter.api.Test;
import vitbuk.com.Ambotorix.entities.Leader;
import vitbuk.com.Ambotorix.matching.LeaderMatcher.MatchResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderMatcherTest {

    private final LeaderMatcher matcher = new LeaderMatcher();

    private static Leader leader(String fullName, String shortName) {
        return new Leader(fullName, shortName, "desc", "pic");
    }

    // A slice of the real Civ6 roster, including the genuinely colliding names.
    private static final List<Leader> LEADERS = List.of(
            leader("Macedon Alexander", "alexander"),
            leader("Austria Maria Theresa", "theresa"),
            leader("America Teddy Roosevelt (Bull Moose)", "roosevelt_bull_moose"),
            leader("America Teddy Roosevelt (Rough Rider)", "roosevelt_rough_rider"),
            leader("America Abraham Lincoln", "lincoln"),
            leader("England Eleanor of Aquitaine (England)", "aquitaine_england"),
            leader("France Eleanor of Aquitaine (France)", "aquitaine_france"),
            leader("China Qin (Unifier)", "qin_unifier"),
            leader("China Qin (Mandate of Heaven)", "qin_mandate_of_heaven"),
            leader("Gran Colombia Simón Bolívar", "bolivar"),
            leader("Vietnam Bà Triệu", "triu"),
            // "Nzinga" is the shortName of one leader but also a name-word of the other.
            leader("Kongo Mvemba a Nzinga", "nzinga"),
            leader("Kongo Nzinga Mbande", "mbande")
    );

    private MatchResult match(String q) {
        return matcher.match(q, LEADERS);
    }

    private void assertUnique(String q, String expectedFullName) {
        MatchResult r = match(q);
        assertInstanceOf(MatchResult.Unique.class, r, "expected unique match for '" + q + "' but got " + r);
        assertEquals(expectedFullName, ((MatchResult.Unique) r).leader().getFullName());
    }

    private void assertAmbiguous(String q, int expectedCount) {
        MatchResult r = match(q);
        assertInstanceOf(MatchResult.Ambiguous.class, r, "expected ambiguous match for '" + q + "' but got " + r);
        assertEquals(expectedCount, ((MatchResult.Ambiguous) r).leaders().size());
    }

    @Test
    void exactTokenMatchIsUnique() {
        assertUnique("alexander", "Macedon Alexander");
        assertUnique("lincoln", "America Abraham Lincoln");
    }

    @Test
    void prefixMatchIsUnique() {
        assertUnique("alex", "Macedon Alexander");
    }

    @Test
    void fuzzyMatchToleratesTypos() {
        assertUnique("teresa", "Austria Maria Theresa");   // missing 'h'
        assertUnique("lincon", "America Abraham Lincoln"); // missing 'l'
    }

    @Test
    void sharedNamesAreAmbiguous() {
        assertAmbiguous("roosevelt", 2);  // both Teddys
        assertAmbiguous("eleanor", 2);    // England + France
        assertAmbiguous("qin", 2);        // both Qins
    }

    @Test
    void civNameIsMatchableAndAmbiguousWhenShared() {
        assertAmbiguous("america", 3);    // Lincoln + two Roosevelts
        assertUnique("macedon", "Macedon Alexander");
    }

    @Test
    void diacriticsAreIgnored() {
        assertUnique("bolivar", "Gran Colombia Simón Bolívar");
        assertUnique("trieu", "Vietnam Bà Triệu");
    }

    @Test
    void multiWordQueryMatchesVariant() {
        assertUnique("bull moose", "America Teddy Roosevelt (Bull Moose)");
    }

    @Test
    void garbageOrTooShortIsNone() {
        assertInstanceOf(MatchResult.None.class, match("a"));
        assertInstanceOf(MatchResult.None.class, match("zzzzzz"));
    }

    @Test
    void exactShortNameBeatsNameWordCollision() {
        // "nzinga" is the exact shortName of Mvemba a Nzinga, but the word "Nzinga" also appears
        // in Nzinga Mbande's full name. An exact shortName match is unambiguous and must win
        // outright — the bot should ban it directly, never ask the user to disambiguate.
        assertUnique("nzinga", "Kongo Mvemba a Nzinga");
        assertUnique("mbande", "Kongo Nzinga Mbande");
    }

    @Test
    void exactTierBeatsFuzzyTier() {
        // "bob" exactly matches Bob; it is also one edit from "bub". Exact must win → Unique(Bob),
        // never Ambiguous.
        List<Leader> pair = List.of(leader("Test Bob", "bob"), leader("Test Bub", "bub"));
        MatchResult r = matcher.match("bob", pair);
        assertInstanceOf(MatchResult.Unique.class, r);
        assertEquals("Test Bob", ((MatchResult.Unique) r).leader().getFullName());
    }
}
