# Scenario tests (`.chat` files)

Beyond the plain unit tests (`services/`, `draft/`), this module has a **scenario harness** that drives
the *whole bot* through realistic chat sessions and checks what it sends back. A scenario is a single
text file that reads like a transcript:

```
--- SCENARIO: open draft (default public mode) ---

--- CHAT ---
alice: /lobby
ambotorix: Lobby created by alice
bob: /register
ambotorix: Player bob added to lobby*
```

Each `*.chat` file under [`resources/scenarios/`](resources/scenarios) becomes one dynamic test in
`ScenarioRunnerTest`. Run them with the rest of the suite:

```bash
./mvnw test                       # everything
./mvnw test -Dtest=ScenarioRunnerTest
```

## How it works

The harness replaces Telegram with a single test double ([`TestTelegramClient`](java/vitbuk/com/Ambotorix/harness/TestTelegramClient.java)):
it feeds the synthesized user input into the real `Ambotorix.consume(...)` entry point and records
every message the bot sends, per chat. Assertions are made **only** on that recorded output — the test
never touches the bot's internals, so scenarios survive refactors and can even describe behavior that
isn't built yet (see *Pending* below).

Tests run against the **real leader data** in `src/main/resources` with the network update disabled
(`data.update.on-startup=false` in [`resources/application.properties`](resources/application.properties)).

## File format

### Sections — who's talking to whom
```
--- CHAT ---          # the shared group chat
--- DM (alice) ---    # alice's private chat with the bot
```
A section sets the chat that the lines beneath it happen in.

### Lines
| Line | Meaning |
|------|---------|
| `alice: /lobby` | actor **alice** sends a message in the current chat |
| `alice: <press_button "Pick *">` | alice taps the most recent button matching the pattern |
| `ambotorix: some text` | expect the bot's next message here to match `some text` |
| `ambotorix:` (empty) | expect **no further** output in this chat (a drain) |
| `# ...` | comment (also allowed at end of a line) |

Actor names are arbitrary (`alice`, `bob`, …); the harness assigns each a stable id. There is one
group chat per scenario.

### Matching a bot message
An `ambotorix:` line is one or more **constructs** that must *all* hold for **one** message:

| Construct | Matches |
|-----------|---------|
| plain text | a text message (exact, after HTML-normalization — see wildcards) |
| `<image>` / `<image {leader}>` | a photo (optionally with a caption matching the pattern) |
| `<button "{leader}">` | a message whose inline buttons **all** match the pattern |
| `<button "{leader}" x6 distinct>` | …and there are **exactly 6**, with **distinct** labels (`xN` / `distinct` optional) |
| `<image> <button "{leader}" x6>` | combine: **one** message that is a photo *and* carries those buttons |

### Wildcards & placeholders
- `*` — any run of characters (`Player bob added to lobby*`).
- `{player}` — any actor name; `{leader}` — any real leader; `{map}` — any map.
- Text is matched after stripping HTML tags and collapsing whitespace, so write
  `ambotorix: @bob registered`, not `<b>@bob</b> registered`. Emoji are kept.

## Matching is strict

- **In order, no skipping.** Each `ambotorix:` line must match the *next* unconsumed message in its
  chat. An unexpected or extra message fails the test (and is printed).
- **Completeness.** Any chat you assert about must be fully accounted for by the end — trailing
  surprises fail. A chat you never mention is out of scope.
- **DM reachability.** Just like real Telegram, the bot can only DM a user who has messaged it
  privately first. So in the default scenario players who only type in the group get their pick pools
  posted publicly (no DMs); to receive a DM a player must open one (e.g. send `/leaders` in their DM)
  — see [`open-draft-dm-opened.chat`](resources/scenarios/open-draft-dm-opened.chat).

## Adding a new scenario

1. Drop a `your-scenario.chat` file into [`resources/scenarios/`](resources/scenarios).
2. Start with a `--- SCENARIO: ... ---` title, then sections and lines as above.
3. `./mvnw test -Dtest=ScenarioRunnerTest`. On failure the runner prints the expected predicates and
   the message it actually found, which is usually enough to fix the line.

Tips:
- Don't over-specify: assert the messages that matter. But remember — once you assert *anything* in a
  chat, you must account for *every* message in it (strictness). Use an empty `ambotorix:` to assert a
  chat is silent.
- Use `{...}` placeholders rather than hard-coding random leaders/maps/slot order.

### Pending / not-yet-built behavior
Add a disposition tag under the title to describe behavior that doesn't exist yet:
```
--- SCENARIO: host tunes draft from own DM ---
--- XFAIL ---       # runs, is expected to fail; if it ever passes, the build fails (remove the tag)
--- DISABLED ---    # skipped entirely
```
