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

## Planned

- [ ] `/photochallenge` command
- [ ] Links to mods instead of plain text
- [ ] Smart ban parsing, e.g. `/ban alex` → bans Alexander (Macedonia)
- [ ] Change auto-terminate to 30 minutes after the lobby starts (currently 4 hours). Note the
      config is `lobby.auto-terminate.hours` — switch it to minutes (or seconds) to express 30 min.
