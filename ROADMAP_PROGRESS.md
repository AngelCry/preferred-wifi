# Roadmap Progress — feature/wifi-database-room-v2 (CLEAN BASE)

## Current Branch: feature/wifi-database-room-v2 (derived from origin/feature/shizuku-auto-switch, HEAD 280e0f547e9b1894ad89bf0f2b5816fb436ee9ed, база 6848cee)

### Legend
- `[ ]` — not started
- `[~]` — in progress / partially
- `[x]` — completed per Definition of Done
- `[!]` — blocked (with reason)

---

### Priority 1: Code-Review Fixes (Phase A) — COMPLETED (A1–A7, все CI success)
- [x] A1. removeNetworkSuggestions: track previous SSIDs — CI run 45301599618 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45301599618)
- [x] A2. Remove System.exit(0) from WifiUserService.destroy() — CI run 45303512810 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45303512810)
- [x] A3. Hidden API calls wrapped in runCatching — verified, no code changes needed, CI run 45306097276 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45306097276)
- [x] A4. Binder IPC off main thread in WifiMonitorService — HandlerThread implemented, CI run 45308514256 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45308514256)
- [x] A5. isMonitoringActive uses flag, not deprecated API — CI run 45309833095 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45309833095)
- [x] A6. bind() after Shizuku permission grant — CI run 45310561769 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45310561769)
- [x] A7. lastAnnouncedNetwork retry after Shizuku reconnect — lastAnnounceWasConnected flag added, CI run 45311319207 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45311319207)

---

### Priority 2: Room Database (Phase B) — NOT STARTED
- [ ] B1. Gradle: kapt plugin + Room dependencies
- [ ] B2. Entity + DAO
- [ ] B3. Migration from SharedPreferences
- [ ] B4. Import system saved networks at startup
- [ ] B5. Scan discovery integration
- [ ] B6. UI: RecyclerView observes Flow from Room
- [ ] B7. WifiNetworkSuggestion only for preferred
- [ ] B8. Editing mode

---

### Priority 3-4: Shizuku & Monitoring & UI — NOT STARTED
- All items [ ] pending

---

## Audit History (recorded for reference)
- **Original HEAD:** ac629c6 — 6 code-review fixes implemented, CI failure (run 45223670063), 8 cancelled runs total
- **Reset:** Branch reset to origin/feature/shizuku-auto-switch (commit 6848cee) via `git reset --hard`
- **Audit branch:** audit/wifi-database-room-v2-failed created from ac629c6 for audit trail
- **Current HEAD:** 280e0f547e9b1894ad89bf0f2b5816fb436ee9ed (A7 applied), база 6848ceea3e7e2ab5103f7a05629cca8efca5d4dc
- **Phase A status:** COMPLETED — A1–A7 verified with green CI
- **Cancelled runs:** 8 total on branch (6 in rejected state: e619178, 106af7c, ee97b99, 683350b, 266fa86, 7498a17; 2 during reset: 1c0a96f, 6848cee). В Phase A отменённых прогонов нет.

---

### Last Updated: 2026-11-24