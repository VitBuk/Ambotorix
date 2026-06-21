# TODO

## Done

- [x] RemoveMap bug (`/mapRemove_<Map>` not working; duplicate maps could be added)
- [x] Add refresh-bans host command (`/clearBans`)
- [x] Decide which commands should be DM-only (host tuning, info commands)
- [x] Reduce chat noise / edit one message instead of posting many
- [x] One combined picture for all picks
- [x] `/help` result in DM

## Planned

- [ ] `/photochallenge` command
- [ ] Links to mods instead of plain text
- [ ] Smart ban parsing, e.g. `/ban alex` → bans Alexander (Macedonia)
- [ ] Open-draft pick pools: send only the image, drop the text caption listing the pools
- [ ] Post the pick-pools image as a reply to the status message, so players can quickly check picks and slot order
- [ ] Tag all players when the host starts the lobby (once the pick image is available) so they get pinged to start thinking about picks
- [ ] Audit group-channel service messages and cut the spam. The single live status message
      already carries lobby state, so most standalone group posts are redundant — they should
      either disappear or become a reply to the status message. Revisit each:
  - "🎮 Draft started by @host! Slot order and map are up ☝️" (`sendStart`) — drop it; instead
    post the picks picture as a reply to the status message (one ping, not two).
  - "🎉 All picks are in! See the reveal ☝️" (secret draft `onAllPicksIn`) — keep only if it
    adds value over the reveal itself; otherwise fold into the reveal post.
  - Secret-draft group notices (`SecretDraftStrategy` lines ~38/49) — check whether these are
    needed in the group or belong in DM.
  - Decide the rule: which moments genuinely deserve a group ping vs. a silent status edit.
- [ ] Change auto-terminate to 30 minutes after the lobby starts (currently 4 hours). Note the
      config is `lobby.auto-terminate.hours` — switch it to minutes (or seconds) to express 30 min.
