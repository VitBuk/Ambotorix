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
        /** The forum topic the step acts in: null = General topic / DM, else a synthetic topic id. */
        Integer threadId();

        record Say(Actor from, long chatId, Integer threadId, String text) implements Step {}
        record Tap(Actor from, long chatId, Integer threadId, String buttonGlob) implements Step {}
        record ExpectMessage(long chatId, Integer threadId, List<Predicate> predicates) implements Step {}
        record ExpectDrain(long chatId, Integer threadId) implements Step {}
        /** Assert the channel's single live (edited) status message currently matches {@code pattern}; non-consuming. */
        record ExpectStatus(long chatId, Integer threadId, String pattern) implements Step {}

        /** A condition on a single recorded message. */
        sealed interface Predicate {
            record Text(String pattern) implements Predicate {}
            /** Inline keyboard: every button matches {@code glob}; {@code count} (if set) is exact; {@code distinct} requires unique labels. */
            record Button(String glob, Integer count, boolean distinct) implements Predicate {}
            record Image(String captionGlob) implements Predicate {}
            /** The channel's live status message (silently edited in place) currently matches {@code pattern}. */
            record Status(String pattern) implements Predicate {}
            /** The message backlinks to the channel's status message (it is a reply to it). */
            record Backlink() implements Predicate {}
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
    private static final Pattern TOPIC = Pattern.compile("(?i)^TOPIC\\s*\\(\\s*(.+?)\\s*\\)$");
    private static final Pattern SCENARIO = Pattern.compile("(?i)^SCENARIO:\\s*(.+)$");
    private static final Pattern QUOTED = Pattern.compile("\"(.*)\"");

    public static Scenario parse(String dsl) {
        String name = "unnamed";
        Disposition disposition = Disposition.RUN;
        Map<String, Actor> actors = new LinkedHashMap<>();
        Map<String, Integer> topics = new LinkedHashMap<>();
        List<Step> steps = new ArrayList<>();
        long[] nextActorId = {1L};
        int[] nextTopicId = {1};
        long currentChat = GROUP_CHAT_ID;
        Integer currentThread = null; // null = General topic of the group (or a DM)

        for (String rawLine : dsl.split("\n", -1)) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;

            Matcher h = HEADER.matcher(line);
            if (h.matches()) {
                String inner = h.group(1).trim();
                Matcher sc = SCENARIO.matcher(inner);
                Matcher dm = DM.matcher(inner);
                Matcher topic = TOPIC.matcher(inner);
                if (sc.matches()) {
                    name = sc.group(1).trim();
                } else if (inner.equalsIgnoreCase("CHAT")) {
                    currentChat = GROUP_CHAT_ID;
                    currentThread = null;
                } else if (topic.matches()) {
                    currentChat = GROUP_CHAT_ID;
                    currentThread = topicOf(topic.group(1).trim(), topics, nextTopicId);
                } else if (dm.matches()) {
                    currentChat = actorOf(dm.group(1).trim(), actors, nextActorId).userId();
                    currentThread = null;
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
                    steps.add(new Step.ExpectDrain(currentChat, currentThread));
                } else {
                    List<Step.Predicate> preds = parsePredicates(payload);
                    if (preds.size() == 1 && preds.get(0) instanceof Step.Predicate.Status st) {
                        steps.add(new Step.ExpectStatus(currentChat, currentThread, st.pattern()));
                    } else if (preds.stream().anyMatch(p -> p instanceof Step.Predicate.Status)) {
                        throw new IllegalArgumentException("<status> must be the only construct on its line: " + payload);
                    } else {
                        steps.add(new Step.ExpectMessage(currentChat, currentThread, preds));
                    }
                }
            } else {
                Actor actor = actorOf(role, actors, nextActorId);
                if (payload.startsWith("<press_button")) {
                    steps.add(new Step.Tap(actor, currentChat, currentThread, extractGlob(payload)));
                } else {
                    steps.add(new Step.Say(actor, currentChat, currentThread, payload));
                }
            }
        }
        return new Scenario(name, disposition, GROUP_CHAT_ID, actors, steps);
    }

    /** A named forum topic gets a stable synthetic {@code message_thread_id} for the scenario. */
    private static Integer topicOf(String name, Map<String, Integer> topics, int[] nextTopicId) {
        return topics.computeIfAbsent(name, n -> nextTopicId[0]++);
    }

    private static Actor actorOf(String name, Map<String, Actor> actors, long[] nextActorId) {
        return actors.computeIfAbsent(name, n -> new Actor(n, nextActorId[0]++));
    }

    /**
     * Parse an {@code ambotorix:} payload into the predicates a single message must satisfy. Plain text
     * runs become {@code Text} predicates; recognised {@code <...>} tokens become their construct, so a
     * line can interleave both, e.g. {@code @bob picked {leader} <backlink>}.
     */
    private static List<Step.Predicate> parsePredicates(String payload) {
        List<Step.Predicate> preds = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < payload.length()) {
            char c = payload.charAt(i);
            if (c == '<') {
                int end = constructEnd(payload, i);
                if (end >= 0) {
                    Step.Predicate p = parseConstruct(payload.substring(i, end + 1));
                    if (p != null) {                 // a recognised construct
                        flushText(text, preds);
                        preds.add(p);
                        i = end + 1;
                        continue;
                    }
                }
            }
            text.append(c);                          // ordinary char (or a non-construct '<')
            i++;
        }
        flushText(text, preds);
        if (preds.isEmpty()) preds.add(new Step.Predicate.Text(payload));
        return preds;
    }

    private static void flushText(StringBuilder text, List<Step.Predicate> preds) {
        String t = text.toString().trim();
        if (!t.isEmpty()) preds.add(new Step.Predicate.Text(t));
        text.setLength(0);
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
            case "status" -> new Step.Predicate.Status(glob);
            case "backlink" -> new Step.Predicate.Backlink();
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
