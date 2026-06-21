package vitbuk.com.Ambotorix.photochallenge;

/** One row of the photo-challenge leaderboard. */
public record Standing(String name, int total, int leaders, int cityStates, int wonders) {}
