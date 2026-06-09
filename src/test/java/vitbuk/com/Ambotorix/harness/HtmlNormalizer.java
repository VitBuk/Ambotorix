package vitbuk.com.Ambotorix.harness;

/**
 * Normal form used to compare bot output against scenario expectations.
 *
 * The bot sends parseMode=HTML, so a raw payload looks like {@code <b>@bob</b> registered}.
 * Normalizing strips HTML tags, unescapes the handful of entities Telegram cares about, trims, and
 * collapses internal whitespace runs to single spaces. Emoji are preserved.
 *
 * Applied to both sides (recorded text and the expected line's literal segments). Images are not an
 * HTML concern here — a sent photo is asserted via the {@code <image …>} DSL construct (an
 * {@code ExpectImage} step), mirroring {@code <button …>}.
 */
public final class HtmlNormalizer {

    private HtmlNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("<[^>]+>", "");       // strip tags
        s = s.replace("&lt;", "<")
             .replace("&gt;", ">")
             .replace("&quot;", "\"")
             .replace("&#39;", "'")
             .replace("&amp;", "&");                     // unescape (& last)
        return s.replaceAll("\\s+", " ").trim();         // collapse whitespace
    }
}
