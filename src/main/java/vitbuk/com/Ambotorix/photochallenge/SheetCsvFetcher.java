package vitbuk.com.Ambotorix.photochallenge;

/**
 * Fetches the raw CSV of the photo-challenge leaderboard sheet. Behind an interface so tests can
 * supply canned CSV instead of hitting the network.
 */
public interface SheetCsvFetcher {
    String fetch() throws Exception;
}
