package vitbuk.com.Ambotorix.photochallenge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoChallengeParserTest {

    private final PhotoChallengeParser parser = new PhotoChallengeParser();

    private static final String REAL_CSV = """
            Player,Total,Leaders,City-States,Wonders
            Shu B,4,1,2,1
            Misha B,4,1,2,1
            Danya S,4,1,1,2
            """;

    @Test
    void parsesRowsInOrder() {
        List<Standing> standings = parser.parse(REAL_CSV);
        assertEquals(3, standings.size());
        assertEquals(new Standing("Shu B", 4, 1, 2, 1), standings.get(0));
        assertEquals(new Standing("Danya S", 4, 1, 1, 2), standings.get(2));
    }

    @Test
    void locatesColumnsByHeaderNameRegardlessOfOrder() {
        String reordered = """
                Wonders,Player,City-States,Leaders,Total
                1,Shu B,2,1,4
                """;
        List<Standing> standings = parser.parse(reordered);
        assertEquals(new Standing("Shu B", 4, 1, 2, 1), standings.get(0));
    }

    @Test
    void toleratesQuotedFieldsAndWhitespace() {
        String csv = """
                Player, Total , Leaders, City-States, Wonders
                "Smith, Jr.",2,1,1,0
                """;
        List<Standing> standings = parser.parse(csv);
        assertEquals("Smith, Jr.", standings.get(0).name());
        assertEquals(2, standings.get(0).total());
    }

    @Test
    void skipsBlankTrailingRows() {
        String csv = "Player,Total,Leaders,City-States,Wonders\nShu B,4,1,2,1\n,,,,\n";
        List<Standing> standings = parser.parse(csv);
        assertEquals(1, standings.size());
    }

    @Test
    void missingRequiredColumnThrows() {
        String csv = "Player,Total,Leaders,Wonders\nShu B,4,1,1\n"; // no City-States
        assertThrows(IllegalArgumentException.class, () -> parser.parse(csv));
    }

    @Test
    void emptyCsvThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
    }

    @Test
    void rendersAlignedMonospaceTable() {
        List<Standing> standings = List.of(
                new Standing("Shu B", 4, 1, 2, 1),
                new Standing("Vitalik B", 1, 1, 0, 0));
        String out = PhotoChallengeService.renderTable(standings);
        assertTrue(out.contains("<pre>") && out.contains("</pre>"), "wrapped in <pre>");
        assertTrue(out.contains("Photo Challenge"), "has title");
        assertTrue(out.contains("Shu B"), "has player");
        // Header and the two data rows → 3 newlines inside the table body.
        String body = out.substring(out.indexOf("<pre>") + 5, out.indexOf("</pre>"));
        assertEquals(3, body.chars().filter(c -> c == '\n').count());
    }
}
