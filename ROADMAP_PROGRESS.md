# Roadmap Progress Tracking

## Current Branch: feature/wifi-database-room

### Priority 1: Room Database Schema and Entity
- [x] Design Room Entity `WifiNetwork` with fields: ssid (PK), securityType, preferred, saved, source, firstSeen, lastUpdated, lastSeenInScan, bssids
- [x] Design Room DAO with atomic upsert/unique key function (`ON_CONFLICT_REPLACE`)
- [x] Implement Room Database class (`WifiNetworkDatabase`) with `@Database` annotation
- [x] Migrate preferences from SharedPreferences to Room without losing `preferred` marks (performed on first launch, stored in `migration_done` pref)

### Priority 2: Import System Networks at Startup
- [ ] Implement import/sync of system's saved networks into local Room database
- [ ] Handle partial errors safely without corrupting local database
- [ ] Ensure `saved=false` is not massively set for all networks on import error

### Priority 3: Scan Discovery Integration
- [ ] Implement background network discovery via Wi-Fi scan broadcasts
- [ ] Use common upsert mechanism for both import and scan sources
- [ ] Deduplicate records from both sources

### Priority 4: UI Updates
- [ ] Observe Flow<List<WifiNetwork>> from Room via DAO
- [ ] RecyclerView populated from Room database, not from scanResults directly
- [ ] List item shows: SSID, frequency band (2.4/5/6 GHz), `saved` flag, `preferred` toggle

### Priority 5: WifiNetworkSuggestions
- [ ] Create `WifiNetworkSuggestion` only for networks marked as `preferred`
- [ ] Not for every known network

### Priority 6: Remove Old Code/Fix Issues
- [x] Fix `removeNetworkSuggestions(emptyList())` not removing old suggestions - now tracks previous SSIDs in SharedPreferences and passes them to `removeNetworkSuggestions()`
- [ ] Remove `System.exit(0)` from `WifiUserService.destroy()` - located in WifiUserService.kt, needs cleanup
- [ ] Fix `isMonitoringActive()` to not use deprecated `ActivityManager.getRunningServices()` - replace with SharedPreferences flag
- [ ] Move Binder IPC calls to background thread - `connectToSavedNetwork` in WifiMonitorService should use coroutines
- [ ] Explicitly call `ShizukuWifiBridge.bind()` after Shizuku permission grant - verify callback automatically invokes bind

---

*Last updated: {{timestamp}}*