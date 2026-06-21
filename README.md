# Ambotorix

Telegram bot for a local Civilization VI community. It runs leader-draft lobbies in a
group chat: players join a lobby, ban leaders, and the bot hands out pick pools (publicly
for open drafts, or privately via DM for secret drafts), then reveals the results. It also
serves reference info — leaders, maps, recommended mods and settings — and keeps the BBG
leader data up to date by scraping it on a schedule.

This is a pet project.

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
