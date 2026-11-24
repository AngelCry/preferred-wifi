# Roadmap Progress — feature/wifi-database-room-v2 (CLEAN BASE)

## Current Branch: feature/wifi-database-room-v2 (derived from origin/feature/shizuku-auto-switch, HEAD 367d9ffc31dae9ad720cc152f18b871512428529, база 6848cee)

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
- [~] A5. isMonitoringActive uses flag, not deprecated API — CI pending
- [ ] A5. isMonitoringActive uses flag, not deprecated API
- [ ] A6. bind() after Shizuku permission grant
- [ ] A7. Clear lastAnnouncedNetwork on Shizuku state change

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
- **Current HEAD:** 367d9ffc31dae9ad720cc152f18b871512428529 (A4 notification fix applied), база 6848ceea3e7e2ab5103f7a05629cca8efca5d4dc
- **Phase A status:** IN PROGRESS — A1 complete (CI success), A2 complete (CI success), A3 verified (CI success), A4 complete (CI success), A5 in progress (CI pending)

---

### Last Updated: 2026-11-24