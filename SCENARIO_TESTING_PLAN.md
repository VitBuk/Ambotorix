# Plan: Scenario-based testing harness

Status: **design — pending review.** Comment inline; open decisions collected at the bottom (§10).

## 1. Goal

Let us describe a whole bot session as a single linear text file — hard-coded user input and
expected bot output with wildcards/placeholders — and run it as an automated test. The suite serves
two purposes at once:

- **Regression tests** — scenarios describing *current* behavior, expected to pass.
- **Acceptance specs** — scenarios describing *desired future* behavior, marked pending (§7). The
  README TODO list can become pending scenarios that auto-close when implemented.

## 2. Harness architecture

The bot has exactly two seams:

- **Input:** `Ambotorix.consume(Update)` — the single entry point. **Synchronous** (long-poll single
  thread), so every outbound send fires inline before `consume` returns. No async waiting.
- **Output:** every send goes through the injected `TelegramClient.execute(...)`.

**D1 — One test double embodies both seams; everything else is black box.** The test's *entire*
contact with production code is a single class — `TestTelegramClient` — which **implements the output
seam** (records what the bot sends) and **drives the input seam** (synthesizes `Update`s and feeds
them to `Ambotorix.consume`). To the bot it is indistinguishable from Telegram itself: updates flow
in through it, responses flow out through it. Assertions are made **solely** on captured output —
never on internal state (lobby maps, etc.). This is what lets a scenario describe behavior the bot
doesn't have yet, and what keeps scenarios stable across internal refactors.

Three layers, in dependency order:

```
Scenario (data)         parsed from DSL; owns actors, generated ids, resolved steps   §3
   │
ScenarioRunner (driver) walks steps; pure wire logic, knows only Scenario + client    §6
   │
TestTelegramClient      synthesize Update → Ambotorix.consume → [real wiring] →        §4
   (the wire)           execute(...) recorded into per-chat queues → back to driver
```

The driver never touches a Spring bean except to obtain the client; the client is the only thing
that ever calls into the bot. That is the black-box boundary made concrete.

## 3. The `Scenario` object (first-class)

Parsing a `.chat` file produces a `Scenario` — a plain data object that **generates all ids and
intermediate data up front**, so the driver can focus purely on wire logic.

```java
record Scenario(
    String name,
    Disposition disposition,        // RUN | IGNORE | XFAIL  — whole-scenario (§7)
    long groupChatId,               // generated
    Map<String, Actor> actors,      // name -> Actor(userId generated)
    List<Step> steps) {
    static Scenario parse(String dsl);   // resolves sections → per-step chat ids, generates ids
}

record Actor(String name, long userId) {}

sealed interface Step {
    record Say(Actor from, long chatId, String text)   implements Step {} // inbound message
    record Tap(Actor from, long chatId, String btnGlob) implements Step {} // <press_button "..">
    record ExpectText(long chatId, LineMatcher matcher) implements Step {} // ambotorix: <text>
    record ExpectButton(long chatId, String labelGlob)  implements Step {} // ambotorix: <button "..">
    record ExpectDrain(long chatId)                     implements Step {} // empty ambotorix:
}
```

Key point: **"current chat" (`--- CHAT ---` / `--- DM (x) ---`) is resolved at parse time** — every
`Step` already carries the concrete `chatId` it acts on. The runner does not track sections.

## 4. `TestTelegramClient` — the wire (input synthesis + output capture)

A single class implementing `TelegramClient`. **Output** side overrides the `execute(...)` overloads
the app uses (`SendMessage`, `SendPhoto`, `AnswerCallbackQuery`), appends a normalized record, and
returns a stub `Message`. No bytes are kept for photos — only caption + buttons. **Input** side
synthesizes `Update`s and delivers them to the bot.

```java
class TestTelegramClient implements TelegramClient {

    private Consumer<Update> bot;                            // = ambotorix::consume
    private final Map<Long, List<OutboundMessage>> history;  // per-chat append-only (§5)
    private final Set<Long> reachableChats;                  // chats with prior inbound activity

    void bindBot(Consumer<Update> bot) { this.bot = bot; }   // see wiring note below

    // ---- inbound: synthesize + deliver (marks the chat reachable) ----
    void deliverMessage(Actor from, long chatId, String text);          // a typed message
    void deliverTap(Actor from, long shownInChat, Button button);       // an inline-button tap

    // ---- outbound: capture (throws if the target chat is unreachable) ----
    @Override <T,M> T execute(M sendMessage) throws TelegramApiException;    // SendMessage
    @Override Message execute(SendPhoto p)   throws TelegramApiException;    // photo: caption+buttons
    // AnswerCallbackQuery → acknowledged, not a chat send

    List<OutboundMessage> history(long chatId);
}

record OutboundMessage(Kind kind, long chatId, String text, boolean hasPhoto, List<Button> buttons) {}
record Button(String label, String callbackData) {}
```

Why synthesis belongs here: in production Telegram builds the `Update` tree
(`Message`/`CallbackQuery` → `from: User`, `chat: Chat`, text/data) and delivers it. In a test there
is no Telegram, so the double must fabricate those objects with enough fidelity that the bot's guards
and routing read real values — `getFrom().getId()` (admin), `getFrom().getUserName()` (host/
registration), `getChatId()` (which lobby), and for taps `callbackQuery.getMessage().getChatId()` +
`getData()`. Co-locating this with the recorder means the double owns *both* directions of the wire,
which is exactly the seam boundary from D1.

Note on `/pick` and `/mapAdd`: their **callback data encodes the group chat id**, so a button tapped
in a DM still routes to the group lobby with the presser's identity. `deliverTap` only needs to set
`message.chat` to the chat where the button was shown and replay the recorded `callbackData`.

**Wiring note (Spring cycle).** Making the client reference the bot directly would form a cycle
(`Ambotorix → AmbotorixService → TelegramClient → Ambotorix`). Avoided by having the client depend
only on a `Consumer<Update>`, bound to `ambotorix::consume` in test setup (`bindBot`). `@Lazy`
injection of `Ambotorix` is the alternative.

## 5. Per-chat history + cursor (pseudo-consume)

Records are appended to one **random-access list per `chatId`** (the group chat, and each user's
DM), persisting for the whole scenario. Nothing is ever removed. Expectations **pseudo-consume** via
a per-chat **cursor** held by the runner. Matching is **strict**: an `ExpectMessage` must be
satisfied by the message at the cursor (the *next* unconsumed one) — there is no forward-skipping, so
an unexpected/interleaved message fails the test and is reported. `ExpectDrain` (empty `ambotorix:`)
asserts the cursor is at the end; `<press_button>` scans the **whole** list backward and never
touches the cursor. One structure serves both "consume in order" and "look back through history".

This is what makes a *linear* file describe *interleaved* output: `/start` appends a group message
**and** per-player DMs at once; a later `--- DM (bob) ---` section keeps reading bob's DM list from
its own cursor, including buttons produced earlier.

**DM reachability.** A send succeeds only to a chat with prior inbound activity — the group (anyone
has messaged it) or a DM the user has opened (messaged the bot privately). Sends to an unreachable
chat throw `TelegramApiException`, exactly as production does, so scenarios reflect reality: players
who only type in the group never receive DMs (their pick pools are posted publicly instead). To make
a player DM-reachable, the scenario has them send a private message first.

## 6. Test infrastructure & data

**D2 — Committed BBG snapshot; tests load from disk, deployment fetches live.** The leader data
(`civ6_leaders.json`, `leader_shortnames.json`, `leaderImages/*.png`) is committed to the repo as a
fixed snapshot. Tests point `data.dir` at it and **skip the live scrape**; the real deployment still
fetches live from `civ6bbg.github.io`. So scenarios run against **real** leader data (resolves the
old "hand-made fixture sizing" worry — ~50 leaders easily satisfies `available > pickSize × players`,
and `picPath` entries resolve to real images, so draft `/start` composes PNGs without error). The
only requirement is that `picPath` resolves relative to the snapshot location.

**D3 — Test profile neutralizes startup nondeterminism.** A `@SpringBootTest` otherwise runs
`DataUpdateService.checkOnStartup()` (`@PostConstruct`, **network scrape**) and arms `@Scheduled`
jobs (daily cron + 15-min cleanup). Plan:
- `src/test/resources/application.properties` (the real one is gitignored) with dummy `bot.token`,
  `bot.username`, `bot.adminId`, and `data.dir` → committed snapshot.
- `data.update.on-startup=false` — a one-line guard in `checkOnStartup` so tests use the on-disk
  snapshot instead of fetching (defaults to `true`, so deployment is unchanged). This is the
  in-repo-snapshot half of D2.
- Disable scheduling in the test context (don't import the scheduler / `@MockBean` it).

## 7. Scenario DSL

A linear `.chat` file (extension open, D-O3). Lines are processed top to bottom; `Scenario.parse`
turns them into the object in §3.

### Sections
```
--- SCENARIO: secret draft happy path ---   # name; optional XFAIL/IGNORE tag (§7-pending)
--- CHAT ---          # the single shared group chat
--- DM (alice) ---    # private chat with alice (chatId == alice's user id)
```
A section sets the **current chat** for the lines beneath it (resolved into each step's `chatId` at
parse time).

### Lines
| Form | Meaning |
|---|---|
| `alice: <text>` | inbound message in the current chat from actor `alice` |
| `ambotorix: <text>` | expect an output to the current chat matching `<text>` |
| `ambotorix:` (empty) | **drain assertion** — current chat's queue has no further unmatched output |
| `ambotorix: <button "Pick *">` | a message whose inline buttons **all** match `Pick *` (≥1). Add a quantifier: `<button "{leader}" x6 distinct>` = **exactly 6**, all matching, labels **distinct** |
| `ambotorix: <image {leader}>` | a **photo** whose caption (alt text) matches the pattern; bare `<image>` matches any photo |
| `ambotorix: <image> <button "{leader}" x6 distinct>` | combine constructs → **one** message satisfying all of them (a photo carrying exactly 6 distinct per-leader buttons) |
| `ambotorix: <status "*players*">` | assert the channel's **single live status message** (posted silently, then edited in place) currently matches the pattern. **Non-consuming** (separate from the stream cursor) and implicitly asserts the status message was created **exactly once** — i.e. edited, not re-posted. Must be the only construct on its line |
| `ambotorix: @bob registered <backlink>` | the message is a **reply that backlinks** to the channel's status message (its `reply_to_message_id` is the status message). Combine with text/other predicates on one line |
| `alice: <press_button "Pick *">` | action — scan current chat **backward** for the most recent message with a button matching the glob, replay its callback data as a tap from `alice` |
| `# ...` / trailing `# ...` | comment |

### Actors
Implicit — any name appearing as `name:` is an actor; `Scenario.parse` assigns each a stable
synthetic user id and creates the one group chat. Admin commands (`/adminLobbies`,
`/adminTerminate`) are **out of scope** — the suite targets drafting flows — so no actor ever needs
to match `bot.adminId`.

### Text matching — D4
- A `ambotorix:` line is an **exact, whole-line match** after **HTML normalization**, where `*` (any run
  of characters) and `{kind}` (a set placeholder) are the only wildcards.
- **HTML normalization** (applied to both recorded text and expected line): strip HTML tags,
  unescape entities, trim, collapse internal whitespace to single spaces. **Emoji preserved.** So
  `ambotorix: @bob registered` matches a raw `<b>@bob</b> registered`. (Trade-off: bold/code formatting
  cannot be asserted. Accepted.)
- Want substring instead of exact? Write `*lobby created*`.

### Placeholders — D5
`{player}`, `{leader}`, `{map}` each match **any** token of that kind (player = any declared actor;
leader = any snapshot leader; map = any `CivMap`). To start, no distinctness/permutation enforcement
— `slot order: {player}, {player}` matches any two player tokens; permutation correctness is covered
at the unit-test level.

### Matching semantics — D6
- **Strict, in order.** Each `ambotorix:` line must be satisfied by the *next* unconsumed message in
  its chat; a mismatch fails (reporting the message that was actually next). No skipping — every
  message in a chat you assert about must be accounted for.
- **One line, one message, N predicates.** A line carries one or more constructs that must *all* hold
  for that single message: plain text → a `Text` predicate; `<button "g" [xN] [distinct]>` → buttons
  all match `g`, optionally exactly `N` of them and/or with distinct labels; `<image g>` → a photo
  whose caption matches `g`. Combine them, e.g. `<image> <button "{leader}" x6 distinct>` asserts one
  message that is an image carrying exactly six distinct per-leader buttons. (The button quantifier is
  the templating primitive for keyboards; per-row or heterogeneous-list templates could extend it.)
- **Completeness.** Any chat the scenario asserts about must be fully consumed by the end (trailing
  surprises fail). An empty `ambotorix:` asserts a chat is drained at that point.
- On failure the runner reports the expected predicates, the chat, and the message it found.

### Pending / aspirational scenarios — D7
A scenario may describe behavior not yet built. Disposition is a **whole-scenario** header tag
(never per-step). Because scenarios are `DynamicTest`s generated by a `@TestFactory`, disposition is
data-driven and maps onto JUnit's **programmatic** primitives, not method/class annotations
(`@Disabled`, `@EnabledIf`, … can't target an individual generated dynamic test):
- `--- DISABLED ---` → the dynamic test calls `Assumptions.abort(...)` → reported **skipped**,
  identical in status to `@Disabled`. Fully framework-native.
- `--- XFAIL ---` → **runs**; an *expected* failure is swallowed (reported skipped/aborted), an
  *unexpected pass* fails the build ("spec now passes — remove the marker"). JUnit/Spring have no
  native expected-to-fail, so this is a ~5-line wrapper. Optional — drop it and use `DISABLED` for
  all pending specs if the auto-nudge-on-pass isn't wanted (D-O4).

## 8. Worked example

The ideal sample expressed in the final semantics. Note disposition is per *scenario*: the
"host tunes draft from own DM" scenario is `XFAIL` as a whole, because today's bot keys host
commands by chat and has no lobby in a DM.

```
--- SCENARIO: secret draft happy path ---

--- CHAT ---
alice: /lobby
ambotorix: *lobby created*
bob: /register
ambotorix: @bob registered

--- CHAT ---
alice: /setDraft secret
ambotorix: *secret*

--- CHAT ---
alice: /start
ambotorix: slot order: {player}, {player}

--- DM (bob) ---
ambotorix: <button "Pick *">
bob: <press_button "Pick *">
ambotorix: You picked {leader}*
```

```
--- SCENARIO: non-host cannot start ---
--- CHAT ---
alice: /lobby
bob: /start
ambotorix: *host*
ambotorix:                      # nothing else — draft did not run
```

```
--- SCENARIO: host tunes draft from own DM ---
--- XFAIL ---             # whole scenario: requires user->lobby routing; not built yet
--- DM (alice) ---
alice: /setDraft secret
ambotorix: *draft*
```

## 9. Phasing & layout

- **Phase 0** — `application.properties` + committed BBG snapshot (§6); `TestTelegramClient` (input
  synthesis + capture + per-chat queues, §4–5); `data.update.on-startup` guard; scheduling off.
- **Phase 1** — one scenario *hardcoded in Java* driving `TestTelegramClient` and asserting on its
  queues, proving the full seam end-to-end before any parser work.
- **Phase 2** — `Scenario.parse` (§3) + `LineMatcher` + a `ScenarioRunner` `@TestFactory` emitting
  one dynamic test per `.chat` file, with state reset between scenarios.
- **Phase 3** — port real flows (lobby/register/ban/open+secret draft/host+player guards) into
  scenario files; convert chosen README TODOs into XFAIL specs.

Proposed layout:
```
src/test/java/.../harness/TestTelegramClient.java     (wire: synth in + record out)
src/test/java/.../harness/HtmlNormalizer.java
src/test/java/.../scenario/Scenario.java              (Scenario, Actor, Step)
src/test/java/.../scenario/LineMatcher.java
src/test/java/.../scenario/ScenarioRunner.java        (@TestFactory; pure wire logic)
src/test/resources/application.properties
src/test/resources/bbg-snapshot/civ6_leaders.json (+ shortnames + leaderImages/*.png)
src/test/resources/scenarios/*.chat
```

## 10. Open decisions

- **~~D-O1~~** — *Resolved (D2):* `data.update.on-startup` guard + committed snapshot (no `@MockBean`).
- **~~D-O2~~** — *Resolved:* admin commands are out of scope (drafting-focused suite); no admin actor
  needed.
- **~~D-O3~~** — *Resolved:* `src/test/resources/scenarios/*.chat`.
- **D-O4** — *Leaning:* `DISABLED` via native `Assumptions.abort`; `XFAIL` via a ~5-line wrapper (no
  native expected-to-fail exists). **Confirm:** keep `XFAIL`'s auto-close-on-pass, or `DISABLED`-only?
- **~~D-O5~~** — *Resolved:* single implicit group for v1; named `--- CHAT (name) ---` deferred.
- **~~D-O6~~** — *Resolved:* `<press_button>` scans the **current chat** only.
