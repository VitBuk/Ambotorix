# TODO

## Done

- [x] RemoveMap bug (`/mapRemove_<Map>` not working; duplicate maps could be added)
- [x] Add refresh-bans host command (`/clearBans`)
- [x] Decide which commands should be DM-only (host tuning, info commands)
- [x] Reduce chat noise / edit one message instead of posting many
- [x] One combined picture for all picks
- [x] `/help` result in DM
- [x] Open-draft pick pools: send only the image (caption is now just @-mentions, not the pool listing)
- [x] Post the pick-pools image as a reply to the status message, so players can quickly check picks and slot order
- [x] Tag all players when the host starts the lobby so they get pinged to start thinking about picks
- [x] Audit group-channel service messages and cut the spam. Removed the standalone "🎮 Draft
      started" milestone; open draft now pings once via the picks image (reply to status, @-mentions
      caption), secret draft pings once via a tag reply to status. Kept the "🎉 All picks are in"
      reveal milestone and the secret-draft DM-failure fallback notices.
- [x] Auto-terminate lobbies 30 minutes after start (was 4 hours); config is now
      `lobby.auto-terminate.minutes`, cleanup check runs every 5 minutes.
- [x] Smart bans: `/ban alex` → Alexander, `/ban teresa` → Theresa (typo-tolerant). Tiered
      exact→prefix→fuzzy matching; shared names (Roosevelt, Eleanor, Qin) offer DM buttons
      instead of guessing.
- [x] `/photochallenge` — reads the public Google Sheet (Main tab CSV) and posts the
      leaderboard (Player/Total/Leaders/City-States/Wonders) as a monospace table to the group.

## Planned

- [ ] Links to mods instead of plain text
- [ ] Never offer a button for an already-banned leader — in any path. `/banButtons` already
      filters them, but smart-ban disambiguation buttons do not; exclude banned leaders from the
      candidate list there too.
