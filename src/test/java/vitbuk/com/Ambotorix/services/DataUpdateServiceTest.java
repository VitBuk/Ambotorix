package vitbuk.com.Ambotorix.services;

import org.junit.jupiter.api.Test;
import vitbuk.com.Ambotorix.scipts.LeaderScraper;
import static org.junit.jupiter.api.Assertions.*;

class DataUpdateServiceTest {

    @Test
    void generateShortName_usesLastWord() {
        assertEquals("lincoln", LeaderScraper.generateShortName("America Abraham Lincoln"));
    }

    @Test
    void generateShortName_withParens_appendsParenContent() {
        assertEquals("saladin_vizier", LeaderScraper.generateShortName("Arabia Saladin (Vizier)"));
        assertEquals("saladin_sultan", LeaderScraper.generateShortName("Arabia Saladin (Sultan)"));
    }

    @Test
    void generateShortName_multiWordParens_joinsWithUnderscore() {
        assertEquals("victoria_age_of_empire",
            LeaderScraper.generateShortName("England Victoria (Age of Empire)"));
    }

    @Test
    void generateShortName_lowercasesResult() {
        assertEquals("curtin", LeaderScraper.generateShortName("Australia John Curtin"));
    }

    @Test
    void isVersionNewer_returnsTrueWhenHigher() {
        assertTrue(DataUpdateService.isVersionNewer("7.5", "7.6"));
        assertTrue(DataUpdateService.isVersionNewer("7.5", "8.0"));
        assertFalse(DataUpdateService.isVersionNewer("7.5", "7.5"));
        assertFalse(DataUpdateService.isVersionNewer("7.6", "7.5"));
    }
}
