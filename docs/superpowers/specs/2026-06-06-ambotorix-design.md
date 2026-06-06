# Ambotorix Bot — Full Design Spec
Date: 2026-06-06

## Overview

Ambotorix is a Telegram bot for a local Civilization 6 community (~15 players). It manages game lobbies: player registration, leader banning, draft picks, and map selection. The bot uses BBG (Better Balanced Game) mod data to keep leader information current.

This spec covers: updating the project to a shippable state, a proper data update pipeline, an extensible draft strategy system, lobby configuration, multi-lobby support (one per Telegram chat), bug fixes, refactoring, and future deployment.

---

## 1. Architecture

### Existing structure (keep as-is)
- `Ambotorix` — Spring long-polling bot, routes updates to `CommandFactory`
- `CommandFactory` — maps command prefixes to `Command` implementations
- `Command` interface — `getInfo()` + `execute(update, service)`
- `HostCommand` / `PlayerCommand` marker interfaces — checked in `Ambotorix.consume()`
- `AmbotorixService` — orchestrates all business logic
- `MarkupService` — builds Telegram inline keyboards
- `PickImageGenerator` — generates leader pick collage images

### New additions
- `DataUpdateService` — BBG version check, scraping, shortName mapping, scheduling
- `NotificationService` — Telegram DM + email alerts on errors
- `DraftStrategy` interface + `DraftStrategyFactory` — pluggable draft logic
- `OpenDraftStrategy` — current behavior (fixed)
- `SecretDraftStrategy` — private picks, simultaneous reveal
- `AdminCommand` marker interface — admin-only commands checked against `admin.telegram.id`
- `/pick` command — new `PlayerCommand` + `DynamicCommand`
- `/status`, `/lobbyInfo`, `/setBanSize`, `/setPickSize`, `/setDraft` commands
- `/adminLobbies`, `/adminTerminate` commands

### Lobby scope
One lobby per Telegram chat, keyed by `chatId` (Long). `LobbyService` changes from holding a single `Lobby` to `Map<Long, Lobby>`. All methods accept a `chatId` parameter. Within a single chat, one active lobby at a time.

---

## 2. Data Update Pipeline

### Version detection
- Fetch `https://civ6bbg.github.io/index.html`, parse current BBG version string (e.g. "7.5")
- Store last-known version in `src/main/resources/bbg_version.txt`
- On check: fetch → compare → skip if same → re-scrape if newer

### Scheduling
- `@PostConstruct` in `DataUpdateService`: runs a version check on startup (fast — no scrape unless version changed)
- `@Scheduled` daily at 3:00 AM (configurable via `data.update.cron` in `application.properties`)
- `/update` command: any user can trigger the same logic manually; bot replies with result

### ShortName mapping file
- New file: `src/main/resources/leader_shortnames.json`
- Format: `{ "America Abraham Lincoln": "lincoln", "Arabia Saladin (Vizier)": "saladin_vizier", ... }`
- When scraping a leader:
  - If a mapping exists → use it
  - If it's a new leader not in the file → auto-generate: take the last meaningful word, lowercased (e.g. `"America Abraham Lincoln"` → `"lincoln"`)
  - Append new auto-generated entries to the file so you can review and edit them
- `civ6_leaders.json` is always the *output* of a scrape — never edit it manually
- `leader_shortnames.json` is your manual control point

### Scraper fix
- Change scraper URL from `index.html` to the versioned leaders page: `https://civ6bbg.github.io/en_US/leaders_{version}.html`
- Parse leader sections from that page (same HTML structure, just version-specific URL)

### On failure
If any step of the update throws, `NotificationService` sends:
1. Telegram DM to `admin.telegram.id` (configured in env vars)
2. Email via Spring `JavaMailSender` (Gmail SMTP + app password, credentials in env vars)

Message includes: failed step, exception summary, BBG version being fetched.

---

## 3. Draft Strategy System

### Interface
```java
public interface DraftStrategy {
    String getName();     // "open", "secret", etc.
    void execute(Lobby lobby, Update update, AmbotorixService service);
}
```

Each strategy is a Spring `@Component`. `DraftStrategyFactory` (same pattern as `CommandFactory`) auto-discovers all strategies by collecting the `List<DraftStrategy>` Spring bean. Adding a new strategy = one new `@Component` class, nothing else changes.

### OpenDraftStrategy
1. Filter out banned leaders from the full pool
2. Shuffle remaining leaders
3. Assign each player `pickSize` unique leaders (fixing the existing bug — uses filtered list)
4. Post to group chat: slot order + chosen map + each player's collage image with description buttons

### SecretDraftStrategy
1. Same filtering and assignment as Open
2. For each player: send a private DM sequence — one `SendPhoto` message per leader in their pool:
   - Leader image (photo)
   - Caption: leader full name
   - Inline keyboard: `[Description]` button (→ `/d shortName`) and `[Pick]` button (→ `/pick shortName`)
3. Exact visual style (caption formatting, button labels, spacing) to be tuned during implementation with real Telegram previews
4. When player presses Pick → `/pick shortName` records selection in `Lobby.pendingPicks`; bot DMs confirmation "You picked Lincoln"
5. Player cannot pick twice; second pick attempt is rejected with an error message
6. When `pendingPicks.size() == players.size()` → bot posts public reveal to group: everyone's final pick shown simultaneously
7. If a player has not started a DM with the bot → bot posts a public message in group asking them to DM the bot first (Telegram limitation: bot cannot initiate DM with a user who hasn't messaged it)

### Lobby changes for draft
```java
// added to Lobby:
private DraftStrategy draftStrategy;          // defaults to OpenDraftStrategy
private Map<String, Leader> pendingPicks;     // username → chosen leader (SecretDraft)
private boolean draftInProgress;              // prevents double /start
private LocalDateTime draftStartedAt;         // for auto-termination
```

### Player entity change
Add `private Long userId` to `Player`. Captured from `update.getMessage().getFrom().getId()` during `/register`. Required for sending private DMs in SecretDraft.

---

## 4. Lobby Configuration

### Commands
| Command | Type | Example | Description |
|---|---|---|---|
| `/lobby` | any | `/lobby` | Create lobby with default settings |
| `/status` | any | `/status` | Show whether a lobby is active (one-liner) |
| `/lobbyInfo` | any in lobby | `/lobbyInfo` | Show full lobby config and player list |
| `/register` | any | `/register` | Join the lobby |
| `/setBanSize` | HostCommand | `/setBanSize 2` | Set bans per player (≥ 0) |
| `/setPickSize` | HostCommand | `/setPickSize 4` | Set leaders per pick pool (≥ 1) |
| `/setDraft` | HostCommand | `/setDraft secret` | Set draft strategy by name |
| `/start` | HostCommand | `/start` | Run the draft |
| `/terminate` | HostCommand | `/terminate` | Close the lobby |

### Default lobby settings
- `pickSize = 6`
- `banSize = 1`
- `draftStrategy = OpenDraftStrategy`
- `mapPool = [Pangea, SevenSeas, Highlands, Lakes]`

### Validation
- `setBanSize`: must be ≥ 0, integer
- `setPickSize`: must be ≥ 1, integer; total leaders needed (`pickSize × playerCount`) is checked at `/start` time (player count isn't known at config time)
- `setDraft`: must match a registered strategy name; bot replies with available names on invalid input

### `/lobbyInfo` output format
```
Lobby by @host | Draft: open
Players (3): host, player2, player3
Pick size: 6 | Bans per player: 1
Map pool: Pangea, Highlands, Lakes, SevenSeas
```

---

## 5. Multi-Lobby & Lifecycle

### One lobby per chat
`LobbyService` holds `Map<Long, Lobby> lobbies`. Every method signature changes from `(args)` to `(Long chatId, args)`. `AmbotorixService` extracts `chatId` from `update` and passes it through.

### Auto-termination
- When `/start` runs: `lobby.draftStartedAt = LocalDateTime.now()`
- `@Scheduled` task runs every 15 minutes, iterates all lobbies
- Any lobby where `draftStartedAt` + 4 hours < now → auto-terminated
- On auto-termination: bot sends group message "Lobby automatically terminated after 4 hours."
- Timeout configurable via `lobby.auto-terminate.hours=4` in `application.properties`

### Manual termination (`/terminate`)
- Clears the lobby from the map
- Cleans up any `pendingPicks`
- Sends group confirmation: "Lobby terminated by @host."

### Admin commands
New `AdminCommand` marker interface — checked in `Ambotorix.consume()` against `admin.telegram.id` config value (admin's Telegram user ID, stored in env vars).

| Command | Description |
|---|---|
| `/adminLobbies` | Lists all active lobbies: chatId, host, player count, age |
| `/adminTerminate [chatId]` | Terminates a specific lobby by chatId |

Admin commands work from any chat including private DMs with the bot.

---

## 6. Bug Fixes

| Bug | Fix |
|---|---|
| `shortName` always `" "` — `/d`, `/ban`, all keyboard buttons broken | ShortName pipeline (Section 2) |
| `sendTerminate()` is empty | Implement: clear lobby, clean up picks, send message |
| Bans ignored in picks — `setLeadersPoolToPlayers` calls `getLeaders()` | Use the filtered `leaders` parameter it receives |
| `LeaderService` constructor takes injected `List<Leader>` and ignores it | Remove parameter, call `loadLeaders()` directly |
| Scraper uses `index.html` instead of versioned leaders URL | Use `leaders_{version}.html` |
| Hardcoded `src/main/resources/...` paths break in JAR | Use Spring `ClassPathResource` / `getClass().getResourceAsStream()` |
| Bot token committed to git in `application.properties` | Move to env vars: `${BOT_TOKEN}`, `${BOT_USERNAME}` |
| `LobbyService.isHost()` has empty `if (lobby == null)` block | Return false when lobby is null |
| Slot order starts at `0` | Change to `1` |
| TelegramBots dependency versions mixed (8.0.0 + 8.2.0) | Align all to `8.2.0` |

---

## 7. Refactoring

- Replace all `System.out.println` with SLF4J (`@Slf4j` + `log.info/warn/error`)
- Remove `DynamicCommand` marker interface — empty, unused in dispatch, dynamic argument parsing happens inside each command's `execute()`
- `Thread.sleep(5000)` in `PickImageGenerator` for temp file cleanup — delete the file immediately after `telegramClient.execute(sendPhoto)` returns
- `mods` and `settings` resource files: move to an external config directory (mountable as Docker volume) so they can be updated without rebuilding the image
- Add `.env.example` to the repo documenting required environment variables
- `leader_shortnames.json` stays in git — it is hand-curated and should be committed. The scraper only appends new entries; it never overwrites existing ones.
- `bbg_version.txt` is a runtime file — add it to `.gitignore`. It is created on first run.

---

## 8. Testing Strategy

Focus on pure business logic — no Telegram API calls needed:

- `LeaderService`: leader pool assignment, ban filtering, shortName lookup, formatDescription
- `LobbyService`: registration, map pool operations, host check, chatId scoping
- `DataUpdateService`: version comparison logic, shortName auto-generation, shortName file merging
- `DraftStrategy` implementations: correct leader assignment, ban exclusion, pending pick tracking

Do not unit-test Telegram API calls. If needed, test `AmbotorixService` methods with a mocked `TelegramClient`.

---

## 9. Deployment (Phase 2 — after local is stable)

**Target:** Hetzner CX11 VPS (~€3.50/month, 2 vCPU, 2GB RAM).

Steps (to be specced separately when ready):
1. Dockerfile — multi-stage build (Maven build → slim JRE image)
2. `docker-compose.yml` — bot container + mounted config/data volume
3. Environment variables via `.env` file on the server
4. GitHub Actions CI: build + push Docker image on merge to main
5. Deployment: `docker-compose pull && docker-compose up -d` on the server

Render.com and Railway.app are valid alternatives if Hetzner feels like too much ops overhead.

---

## 10. New Commands Summary

| Command | Type | Status |
|---|---|---|
| `/update` | any | New |
| `/status` | any | New |
| `/lobbyInfo` | any in lobby | New |
| `/setBanSize [n]` | HostCommand | New |
| `/setPickSize [n]` | HostCommand | New |
| `/setDraft [name]` | HostCommand | New |
| `/pick [shortName]` | PlayerCommand + DynamicCommand | New |
| `/adminLobbies` | AdminCommand | New |
| `/adminTerminate [chatId]` | AdminCommand | New |
| `/terminate` | HostCommand | Exists — implement body |
