# Roadmap Progress — feature/wifi-database-room-v2 (CLEAN BASE)

## Current Branch: feature/wifi-database-room-v2 (derived from origin/feature/shizuku-auto-switch, HEAD f8e273356de72a108b54ab907d010aece1d55941, база 6848cee)

### Legend
- `[ ]` — not started
- `[~]` — in progress / partially
- `[x]` — completed per Definition of Done
- `[!]` — blocked (with reason)

---

### Priority 1: Code-Review Fixes (Phase A) — IN PROGRESS
- [x] A1. removeNetworkSuggestions: track previous SSIDs — CI run 45301599618 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45301599618)
- [x] A2. Remove System.exit(0) from WifiUserService.destroy() — CI run 45303512810 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45303512810)
- [x] A3. Hidden API calls wrapped in runCatching — verified, no code changes needed, CI run 45306097276 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45306097276)
- [x] A4. Binder IPC off main thread in WifiMonitorService — HandlerThread implemented, CI run 45308514256 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45308514256)
- [x] A5. isMonitoringActive uses flag, not deprecated API — CI run 45309833095 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45309833095)
- [x] A6. bind() after Shizuku permission grant — CI run 45310561769 success, artifact preferred-wifi-debug published (https://github.com/AngelCry/preferred-wifi/actions/runs/45310561769)
- [~] A7. lastAnnouncedNetwork retry after Shizuku reconnect — lastAnnounceWasConnected flag added, CI pending

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
- **Original HEAD:** ac629c6 — 6 code-review fixes implemented, CI failure (run 45223670063), 5 cancelled runs
- **Reset:** Branch reset to origin/feature/shizuku-auto-switch (commit 6848cee) via `git reset --hard`
- **Audit branch:** audit/wifi-database-room-v2-failed created from ac629c6 for audit trail
- **Current HEAD:** 4f8ae3b92f0366036c479c546015c67219b40272 (A5 applied), база 6848ceea3e7e2ab5103f7a05629cca8efca5d4dc
- **Phase A status:** IN PROGRESS — A1–A5 complete (all CI success), A6 verified (CI success), A7 in progress (CI pending)

---

### Last Updated: 2026-11-24