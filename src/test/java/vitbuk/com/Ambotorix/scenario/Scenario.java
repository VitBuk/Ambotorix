package vitbuk.com.Ambotorix.scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A scenario parsed from a {@code .chat} DSL file (SCENARIO_TESTING_PLAN.md §3).
 *
 * <p>Parsing generates all ids up front — one group chat ({@link #groupChatId}) plus a synthetic
 * user id per actor — and resolves every {@code --- CHAT ---}/{@code --- DM (x) ---} section into a
 * concrete {@code chatId} on each {@link Step}. The runner walks {@link #steps} as pure wire logic.
 *
 * <p>An {@code ambotorix:} line is an {@link Step.ExpectMessage} carrying one or more
 * {@link Step.Predicate}s that must all hold for a single message — a plain line is one
 * {@code Text} predicate; {@code <image> <button "{leader}">} is {@code Image} + {@code Button}.
 */
public final class Scenario {

    public enum Disposition { RUN, DISABLED, XFAIL }

    public record Actor(String name, long userId) {}

    public sealed interface Step {
        long chatId();

        record Say(Actor from, long chatId, String text) implements Step {}
        record Tap(Actor from, long chatId, String buttonGlob) implements Step {}
        record ExpectMessage(long chatId, List<Predicate> predicates) implements Step {}
        record ExpectDrain(long chatId) implements Step {}

        /** A condition on a single recorded message. */
        sealed interface Predicate {
            record Text(String pattern) implements Predicate {}
            /** Inline keyboard: every button matches {@code glob}; {@code count} (if set) is exact; {@code distinct} requires unique labels. */
            record Button(String glob, Integer count, boolean distinct) implements Predicate {}
            record Image(String captionGlob) implements Predicate {}
        }
    }

    private static final long GROUP_CHAT_ID = 1000L;

    /** The role name that denotes bot output in a scenario (kept distinct from player names). */
    private static final String BOT_ROLE = "ambotorix";

    private final String name;
    private final Disposition disposition;
    private final long groupChatId;
    private final Map<String, Actor> actors;
    private final List<Step> steps;

    private Scenario(String name, Disposition disposition, long groupChatId,
                     Map<String, Actor> actors, List<Step> steps) {
        this.name = name;
        this.disposition = disposition;
        this.groupChatId = groupChatId;
        this.actors = actors;
        this.steps = steps;
    }

    public String name() { return name; }
    public Disposition disposition() { return disposition; }
    public long groupChatId() { return groupChatId; }
    public List<Step> steps() { return steps; }
    public java.util.Collection<String> actorNames() { return actors.keySet(); }

    private static final Pattern HEADER = Pattern.compile("^---\\s*(.*?)\\s*---$");
    private static final Pattern DM = Pattern.compile("(?i)^DM\\s*\\(\\s*(.+?)\\s*\\)$");
    private static final Pattern SCENARIO = Pattern.compile("(?i)^SCENARIO:\\s*(.+)$");
    private static final Pattern QUOTED = Pattern.compile("\"(.*)\"");

    public static Scenario parse(String dsl) {
        String name = "unnamed";
        Disposition disposition = Disposition.RUN;
        Map<String, Actor> actors = new LinkedHashMap<>();
        List<Step> steps = new ArrayList<>();
        long[] nextActorId = {1L};
        Long currentChat = GROUP_CHAT_ID;

        for (String rawLine : dsl.split("\n", -1)) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;

            Matcher h = HEADER.matcher(line);
            if (h.matches()) {
                String inner = h.group(1).trim();
                Matcher sc = SCENARIO.matcher(inner);
                Matcher dm = DM.matcher(inner);
                if (sc.matches()) {
                    name = sc.group(1).trim();
                } else if (inner.equalsIgnoreCase("CHAT")) {
                    currentChat = GROUP_CHAT_ID;
                } else if (dm.matches()) {
                    currentChat = actorOf(dm.group(1).trim(), actors, nextActorId).userId();
                } else if (inner.equalsIgnoreCase("XFAIL")) {
                    disposition = Disposition.XFAIL;
                } else if (inner.equalsIgnoreCase("DISABLED") || inner.equalsIgnoreCase("IGNORE")) {
                    disposition = Disposition.DISABLED;
                }
                continue;
            }

            int colon = line.indexOf(':');
            if (colon < 0) continue; // not a recognised line
            String role = line.substring(0, colon).trim();
            String payload = line.substring(colon + 1).trim();

            if (role.equals(BOT_ROLE)) {
                if (payload.isEmpty()) {
                    steps.add(new Step.ExpectDrain(currentChat));
                } else {
                    steps.add(new Step.ExpectMessage(currentChat, parsePredicates(payload)));
                }
            } else {
                Actor actor = actorOf(role, actors, nextActorId);
                if (payload.startsWith("<press_button")) {
                    steps.add(new Step.Tap(actor, currentChat, extractGlob(payload)));
                } else {
                    steps.add(new Step.Say(actor, currentChat, payload));
                }
            }
        }
        return new Scenario(name, disposition, GROUP_CHAT_ID, actors, steps);
    }

    private static Actor actorOf(String name, Map<String, Actor> actors, long[] nextActorId) {
        return actors.computeIfAbsent(name, n -> new Actor(n, nextActorId[0]++));
    }

    /** Parse an {@code ambotorix:} payload into the predicates a single message must satisfy. */
    private static List<Step.Predicate> parsePredicates(String payload) {
        List<Step.Predicate> constructs = parseConstructs(payload);
        if (constructs != null) return constructs;
        return List.of(new Step.Predicate.Text(payload));
    }

    /** Returns the {@code <...>} constructs on the line, or null if it is plain text. */
    private static List<Step.Predicate> parseConstructs(String payload) {
        if (!payload.startsWith("<")) return null;
        List<Step.Predicate> preds = new ArrayList<>();
        int i = 0;
        while (i < payload.length()) {
            char c = payload.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c != '<') return null;             // text mixed with constructs → treat as plain text
            int end = constructEnd(payload, i);
            if (end < 0) return null;
            Step.Predicate p = parseConstruct(payload.substring(i, end + 1));
            if (p == null) return null;            // unknown construct → treat whole line as text
            preds.add(p);
            i = end + 1;
        }
        return preds.isEmpty() ? null : preds;
    }

    /** Index of the {@code >} that closes the construct starting at {@code start}, ignoring quoted {@code >}. */
    private static int constructEnd(String s, int start) {
        boolean inQuotes = false;
        for (int j = start; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == '"') inQuotes = !inQuotes;
            else if (c == '>' && !inQuotes) return j;
        }
        return -1;
    }

    private static Step.Predicate parseConstruct(String token) {
        String inner = token.substring(1, token.length() - 1).trim(); // strip < >
        String name = inner.split("\\s", 2)[0].toLowerCase();
        String glob = extractGlob(token);
        return switch (name) {
            case "button" -> parseButton(inner, glob);
            case "image" -> new Step.Predicate.Image(glob);
            default -> null;
        };
    }

    /** {@code button "PAT" [xN] [distinct]} — modifiers follow the (quoted or bare) pattern. */
    private static Step.Predicate parseButton(String inner, String glob) {
        Integer count = null;
        boolean distinct = false;
        for (String mod : modifiersAfterPattern(inner).split("\\s+")) {
            if (mod.isEmpty()) continue;
            if (mod.matches("(?i)x\\d+")) count = Integer.parseInt(mod.substring(1));
            else if (mod.equalsIgnoreCase("distinct")) distinct = true;
        }
        return new Step.Predicate.Button(glob, count, distinct);
    }

    /** The text after the construct's pattern: e.g. {@code button "{leader}" x6 distinct} → {@code x6 distinct}. */
    private static String modifiersAfterPattern(String inner) {
        int sp = inner.indexOf(' ');
        if (sp < 0) return "";
        String rest = inner.substring(sp + 1).trim();          // pattern + modifiers
        if (rest.startsWith("\"")) {
            int close = rest.indexOf('"', 1);
            return close < 0 ? "" : rest.substring(close + 1).trim();
        }
        int sp2 = rest.indexOf(' ');
        return sp2 < 0 ? "" : rest.substring(sp2 + 1).trim();
    }

    /** Pull the glob out of {@code <button "Pick *">} / {@code <image {leader}>} / {@code <image>}. */
    private static String extractGlob(String token) {
        Matcher q = QUOTED.matcher(token);
        if (q.find()) return q.group(1);
        String inner = token.replaceAll("^<\\w+", "").replaceAll(">\\s*$", "").trim();
        return inner.isEmpty() ? "*" : inner;
    }

    private static String stripComment(String line) {
        if (line.stripLeading().startsWith("#")) return "";
        return line.replaceAll("\\s+#.*$", "");
    }
}
