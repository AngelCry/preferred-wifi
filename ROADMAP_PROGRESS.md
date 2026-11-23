# Roadmap Progress — feature/wifi-database-room-v2 (CLEAN BASE)

## Current Branch: feature/wifi-database-room-v2 (reset to origin/feature/shizuku-auto-switch)

### Legend
- `[ ]` — not started
- `[~]` — in progress / partially
- `[x]` — completed per Definition of Done
- `[!]` — blocked (with reason)

---

### Priority 1: Code-Review Fixes (Phase A) — NOT STARTED
- [ ] A1. removeNetworkSuggestions: track previous SSIDs
- [ ] A2. Remove System.exit(0) from WifiUserService.destroy()
- [ ] A3. Hidden API calls wrapped in runCatching
- [ ] A4. Binder IPC off main thread in WifiMonitorService
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

## Audit History
- **Original HEAD:** ac629c6 — 6 code-review fixes implemented, CI failure (run 45223670063), 5 cancelled runs
- **Reset:** Branch reset to origin/feature/shizuku-auto-switch (commit 6848cee)
- **Current HEAD:** 6848ceea3e7e2ab5103f7a05629cca8efca5d4dc
- **Audit branch:** audit/wifi-database-room-v2-failed created from ac629c6
- **Purpose:** Reset to clean base, no Room/kapt/code changes, document state

---

### Last Updated: Real date (not {{timestamp}})