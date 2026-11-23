# Roadmap Progress — feature/wifi-database-room-v2

## Current Branch: feature/wifi-database-room-v2 (derived from origin/feature/shizuku-auto-switch)

### Legend
- `[ ]` — not started
- `[~]` — in progress / partially (with explanation of what's missing)
- `[x]` — completed per Definition of Done (requires green CI + acceptance criteria)
- `[!]` — blocked (with reason)

---

### Priority 1: Code-Review Fixes (Phase A)

#### A1. removeNetworkSuggestions: track previous SSIDs
- [~] Store previously added suggestion SSIDs in SharedPreferences (will implement in this step)
- [ ] Build List<WifiNetworkSuggestion> from tracked SSIDs
- [ ] Pass proper List<WifiNetworkSuggestion> to removeNetworkSuggestions (not array of strings)
- [ ] Update prefs-tracking after adding new suggestions
- [ ] Verify old suggestions are removed before adding new ones

#### A2. Remove System.exit(0) from WifiUserService.destroy()
- [ ] Remove System.exit(0) call
- [ ] Lifecycle management handled by Shizuku UserService only

#### A3. Hidden API calls wrapped in runCatching
- [ ] Verify hidden API calls (getConfiguredNetworks, connect) are wrapped in runCatching
- [ ] User receives specific error message, not silent failure
- [ ] If already correct in base branch: mark "verified, no changes needed"

#### A4. Binder IPC off main thread in WifiMonitorService
- [ ] Create background thread (HandlerThread with start() OR CoroutineScope + Dispatchers.IO)
- [ ] Binder operations run on background thread
- [ ] UI updates (notifications) posted to main thread
- [ ] Service does not crash on start
- [ ] HandlerThread started before Handler creation

#### A5. isMonitoringActive uses flag, not deprecated API
- [ ] Flag monitoring_active set when service starts
- [ ] Flag cleared in onDestroy
- [ ] UI reads flag and verifies state in onResume
- [ ] No deprecated getRunningServices() usage

#### A6. bind() after Shizuku permission grant
- [ ] Permission result listener registered in onCreate
- [ ] On granted: explicitly call ShizukuWifiBridge.bind()
- [ ] Listener removal paired with addition (onDestroy ↔ onCreate)

#### A7. Clear lastAnnouncedNetwork on Shizuku state change
- [ ] lastAnnouncedNetwork reset when bridge state changes

---

### Priority 2: Room Database (Phase B)

#### B1. Gradle: kapt plugin + Room dependencies
- [ ] Add `id("org.jetbrains.kotlin.kapt")` plugin
- [ ] Add dependencies: room-runtime, room-ktx, room-compiler
- [ ] CI builds Debug APK successfully

#### B2. Entity + DAO
- [ ] Entity: ssid as @PrimaryKey, securityType nullable, remaining fields per handoff §4
- [ ] DAO: suspend or Flow methods only, called from lifecycleScope/viewModelScope
- [ ] Common upsert: INSERT OR IGNORE + partial UPDATE in @Transaction (no REPLACE)
- [ ] Separate update query for preferred+lastUpdated only
- [ ] Room DAO tests if possible, otherwise manual checklist

#### B3. Migration from SharedPreferences
- [ ] Outside main thread, in transaction
- [ ] migration_done flag set only after success
- [ ] SharedPreferences not cleared
- [ ] Partial error: no mass reset of saved=false for other networks

#### B4. Import system saved networks at startup
- [ ] Add missing records, update existing without duplicates
- [ ] preferred not set automatically
- [ ] Partial error: no reset of other networks' saved, no deletion of local records
- [ ] If system list unavailable on Android 14: log limitation, do not fabricate

#### B5. Scan discovery integration
- [ ] All discovered networks via same upsert mechanism
- [ ] No polling, no startScan()
- [ ] Use existing passive SCAN_RESULTS_AVAILABLE_ACTION receiver

#### B6. UI: RecyclerView observes Flow from Room
- [ ] Flow<List<WifiNetwork>> from DAO
- [ ] List item: SSID, range (2.4/5/6 GHz), saved toggle, preferred toggle
- [ ] preferred toggle updates only preferred+lastUpdated, reflects immediately
- [ ] Manual refresh writes scanResults to DB via upsert, then UI updates from Flow
- [ ] Known networks not lost while DB fills

#### B7. WifiNetworkSuggestion only for preferred
- [ ] Suggestions created only for preferred networks
- [ ] Nothing created for non-preferred networks

#### B8. Editing mode
- [ ] Manual deletion only of local records (do not change system list)
- [ ] Update README with actual limitations (no false promises)

---

### Priority 3: Shizuku & Monitoring

#### A5-A7 continuation from Phase A

---

### Priority 4: UI & Permissions

#### A8. Restore runtime permission requests
- [ ] ACCESS_FINE_LOCATION via Activity Result API
- [ ] NEARBY_WIFI_DEVICES via Activity Result API
- [ ] POST_NOTIFICATIONS via Activity Result API
- [ ] onResume, onDestroy preserved correctly
- [ ] scanResults display maintained

---

## Last Updated: Real date (not {{timestamp}})