package vitbuk.com.Ambotorix.draft;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HersonPickParserTest {

    @Test
    void parsesCanonicalFormat() {
        assertEquals(List.of("Gandhi", "Lincoln", "Saladin", "Trajan"),
                HersonPickParser.parse("1. Gandhi 2. Lincoln 3. Saladin 4. Trajan"));
    }

    @Test
    void toleratesAssortedSeparatorsAndSpacing() {
        assertEquals(List.of("Gandhi", "Lincoln", "Saladin", "Trajan"),
                HersonPickParser.parse("1)Gandhi   2 - Lincoln 3:Saladin   4 Trajan"));
    }

    @Test
    void handlesNewlinesAndMultiWordNames() {
        assertEquals(List.of("Abraham Lincoln", "Saladin Sultan", "Pedro", "Basil"),
                HersonPickParser.parse("1. Abraham Lincoln\n2. Saladin Sultan\n3. Pedro\n4. Basil"));
    }

    @Test
    void rejectsWrongCount() {
        assertTrue(HersonPickParser.parse("1. Gandhi 2. Lincoln 3. Saladin").isEmpty());
        assertTrue(HersonPickParser.parse("just chatting").isEmpty());
        assertTrue(HersonPickParser.parse("").isEmpty());
        assertTrue(HersonPickParser.parse(null).isEmpty());
    }

    @Test
    void rejectsDuplicateOrMissingRanks() {
        assertTrue(HersonPickParser.parse("1. Gandhi 1. Lincoln 2. Saladin 3. Trajan").isEmpty());
        assertTrue(HersonPickParser.parse("1. Gandhi 2. Lincoln 3. Saladin 5. Trajan").isEmpty());
    }
}
