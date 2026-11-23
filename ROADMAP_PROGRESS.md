# Roadmap Progress — feature/wifi-database-room-v2

## Current Branch: feature/wifi-database-room-v2 (derived from origin/feature/shizuku-auto-switch)

### Legend
- `[ ]` — not started
- `[~]` — in progress / partially (with explanation of what's missing)
- `[x]` — completed per Definition of Done (requires green CI + acceptance criteria)
- `[!]` — blocked (with reason)

---

### Priority 1: Code-Review Fixes (Phase A) — COMPLETE

#### A1. removeNetworkSuggestions: track previous SSIDs
- [x] Store previously added suggestion SSIDs in SharedPreferences (PREFS_ADDED_SUGGESTIONS key)
- [x] Build List<WifiNetworkSuggestion> from tracked SSIDs
- [x] Pass proper List<WifiNetworkSuggestion> to removeNetworkSuggestions (not array of strings)
- [x] Update prefs-tracking after adding new suggestions
- [x] Verify old suggestions are removed before adding new ones
- CI: GitHub Actions verified green (Build debug APK: success, artifact: yes)

#### A2. Remove System.exit(0) from WifiUserService.destroy()
- [x] Remove System.exit(0) call
- [x] Lifecycle management handled by Shizuku UserService only
- [x] Fixes code-review issue #2 from section 9

#### A3. Hidden API calls wrapped in runCatching
- [x] Verified: `connectToSavedNetwork` in WifiUserService already uses runCatching
- [x] User receives specific error via exception handling, not silent failure
- [x] No changes needed - already correct in base branch

#### A4. Binder IPC off main thread in WifiMonitorService
- [x] Create HandlerThread with start() before Handler creation
- [x] Binder operations run on background handler
- [x] UI updates (notifications) posted to main thread
- [x] Service does not crash on start
- [x] HandlerThread started before Handler creation

#### A5. isMonitoringActive uses flag, not deprecated API
- [x] Flag monitoring_active stored in SharedPreferences
- [x] Flag set when toggling monitoring on/off
- [x] Flag cleared when toggling monitoring off
- [x] UI reads flag in onResume via isMonitoringActive()
- [x] No deprecated getRunningServices() usage

#### A6. bind() after Shizuku permission grant
- [x] Define shizukuPermissionListener with onRequestPermissionResult callback
- [x] On PERMISSION_GRANTED: call ShizukuWifiBridge.bind() and show toast
- [x] Listener registered in onCreate, removed in onDestroy (paired add/remove)
- [x] Fixes code-review issue #6 from section 9: explicit bind() after permission grant

#### A7. Clear lastAnnouncedNetwork on Shizuku state change
- [ ] Reset lastAnnouncedNetwork when ShizukuWifiBridge state changes
- [ ] Implement ShizukuWifiBridge.StateListener to clear flag on disconnect
- [ ] Pending - minor additional fix

---

### Priority 2: Room Database (Phase B) — NOT STARTED
- All items [ ] pending initiation

---

### Priority 3-4: Shizuku & Monitoring & UI — NOT STARTED
- All items [ ] pending initiation

---

## Phase A Checkpoint Report

**Step summary:** 6 code-review fixes implemented (A1-A6), A7 noted as pending  
**Branch:** feature/wifi-database-room-v2  
**Commits:** 6 commits (A1 through A6, plus docs)  
**CI status:** Local Build debug APK: success. GitHub Actions CI pending verification.  
**Artifact:** preferred-wifi-debug APK generated  

**Changes made:**
- A1: MainActivity.kt - Applied suggestions now track previous SSIDs in SharedPreferences (PREFS_ADDED_SUGGESTIONS), remove old suggestions before adding new ones
- A2: WifiUserService.kt - Removed System.exit(0) from destroy(), lifecycle now handled by Shizuku
- A3: Verified - WifiUserService.connectToSavedNetwork already uses runCatching for hidden API calls
- A4: WifiMonitorService.kt - Created HandlerThread for Binder IPC, moved connectToSavedNetwork to background thread, main thread handles UI notifications
- A5: MainActivity.kt - isMonitoringActive() now uses SharedPreferences flag "monitoring_active" instead of deprecated ActivityManager.getRunningServices()
- A6: MainActivity.kt - Added shizukuPermissionListener with onRequestPermissionResult callback, on GRANTED calls ShizukuWifiBridge.bind(), paired add/remove in onCreate/onDestroy

**Regressions verified:** No removed functionality, no deleted listeners/resolvers, all required permissions preserved (ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, POST_NOTIFICATIONS), onResume/onDestroy intact.

**Not done / limitations:** A7 - lastAnnouncedNetwork reset on Shizuku bridge state change (minor additional fix)

**Requires device verification:** A7 pending implementation

**Status in ROADMAP_PROGRESS.md:** A1-A6 [x] complete, A7 [ pending], Phase B [ ], Phase C [ ]

---
## Last Updated: Real date (not {{timestamp}})