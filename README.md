# Ambotorix

Telegram bot for a local Civilization VI community. It runs leader-draft lobbies in a
group chat: players join a lobby, ban leaders, and the bot hands out pick pools (publicly
for open drafts, or privately via DM for secret drafts), then reveals the results. It also
serves reference info — leaders, maps, recommended mods and settings — and keeps the BBG
leader data up to date by scraping it on a schedule.

This is a pet project.

## Ambotorix Beta 0.3

What's new in this release:

- **Less group-chat spam on draft start.** Removed the standalone "🎮 Draft started" message.
  The draft now pings the group exactly once:
  - **Open draft** posts the combined picks image as a reply to the status message, captioned
    with `@`-mentions of every player (no more pool-listing text — the image shows the pools).
  - **Secret draft** posts a single tag reply to the status message telling players to check
    their DMs to pick.
- Both pings reply to (backlink) the status message, so players can tap to jump to slot order
  and picks.
- Kept the "🎉 All picks are in" reveal and the secret-draft DM-failure fallback notices.
- **Lobbies auto-terminate 30 minutes after start** (was 4 hours). Config key is now
  `lobby.auto-terminate.minutes`; the cleanup check runs every 5 minutes.
- **Smart bans.** `/ban` now accepts loose input: `/ban alex` bans Alexander, `/ban teresa`
  bans Theresa despite the typo. Matching is tiered (exact → prefix → fuzzy), and when a query
  matches several leaders (e.g. `roosevelt`, `eleanor`, `qin`) the bot replies with buttons to
  pick the right one rather than guessing.

## Ambotorix Beta 0.2

What's new in this release:

- **Single live status message.** The lobby keeps one message (settings, players, bans,
  slot order, picks) that is edited in place as things change, instead of posting a new line
  for every join, ban, config tweak and pick. Only milestones ("draft started", "all picks
  in") ping the group.
- **One combined pick-pools image** for open drafts — a row per player — instead of one post
  per player.
- **Quieter group chat.** Info and host-tuning commands (`/help`, `/leaders`, `/maplist`,
  `/mappool`, `/mods`, `/settings`, map/config commands, etc.) now reply in DM instead of
  spamming the group.
- **`/clearBans`** (host) — clears all bans so players can redo them.
- **`/banButtons`** — every registered player gets DM buttons for each not-yet-banned leader;
  tapping one bans that leader and updates the group status message.
- **`/mappool` remove buttons** — tap to remove a map from the pool, sent to DM.
- **Quick start** section at the top of `/help`.
- **Bug fix:** `/mapRemove_<Map>` now works, and maps can no longer be added twice.

See [TODO.md](TODO.md) for what's planned next.

## Running it

The bot is containerised. With Docker and a populated `.env` (bot token, username, admin id):

```bash
docker compose up --build -d
```
