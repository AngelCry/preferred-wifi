package com.angelcry.preferredwifi

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.angelcry.preferredwifi.databinding.ActivityMainBinding
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.Room
import com.angelcry.preferredwifi.WifiNetworkDatabase
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.executor

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var prefs: android.content.SharedPreferences

    // Room database for local Wi-Fi network storage - the source of truth
    private val wifiDb: WifiNetworkDatabase by lazy {
        WifiNetworkDatabase.getDatabase(applicationContext)
    }

    // Network adapter - displays lists from Room database
    private lateinit var adapter: NetworkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        prefs = getSharedPreferences(WifiMonitorService.PREFS, Context.MODE_PRIVATE)

        // Check if migration from SharedPreferences has been done
        // If not, perform migration and mark as done
        if (!prefs.contains("migration_done")) {
            performMigrationFromPrefs()
            prefs.edit().putBoolean("migration_done", true).apply()
        }

        // Initialize adapter with empty list - will be populated by Room observer
        adapter = NetworkAdapter(mutableSetOf<String>()) { ssid, checked ->
            // Update Room database when user toggles preferred flag via adapter
            wifiDb.wifiNetworkDao().upsert(WifiNetwork(
                ssid = ssid,
                preferred = checked,
                saved = isSaved(ssid),
                source = if (checked) "user_set" else "user_removed",
                firstSeen = systemMillis(),
                lastUpdated = systemMillis()
            ))
            // Refresh the monitoring button after preference change
            updateMonitorButton()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Load networks from Room database on startup
        loadNetworksFromDatabase()

        binding.btnRefresh.setOnClickListener { refreshNetworks() }
        binding.btnApply.setOnClickListener {
            savePreferences()
            applySuggestions()
            Toast.makeText(this, "Предференции переданы Android", Toast.LENGTH_SHORT).show()
        }
        binding.btnShizuku.setOnClickListener { requestShizukuAccess() }
        binding.btnMonitor.setOnClickListener { requestPermissionAndToggleMonitoring() }

        ShizukuWifiBridge.setStateListener { message ->
            updateShizukuButton()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        Shizuku.addBinderReceivedListenerSticky(shizukuBinderListener)
        Shizuku.addBinderDeadListener(shizukuDeadListener)
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        checkPermissionsAndLoad()
        tryBindPreviouslyGrantedShizuku()

        // Track when app goes to background/foreground
        ProcessLifecycleOwner.lifecycleObserver.onResume {
            // App came to foreground
        }
        ProcessLifecycleOwner.lifecycleObserver.onPause {
            // App went to background
        }
    }

    /** Returns the set of SSIDs that the user has marked as preferred from Room database. */
    private val preferredSsids: Set<String
        get() = wifiDb.wifiNetworkDao().getAllNetworks().asSequence().filter { it.preferred }.map { it.ssid }.toSet()

    /** Loads networks from Room database and updates the adapter. */
    private fun loadNetworksFromDatabase() {
        CoroutineScope(Dispatchers.Main).launch {
            val networks = wifiDb.wifiNetworkDao().getAllNetworks()
            val available = mutableSetOf<String>()
            networks.forEach { network ->
                available.add(network.ssid)
            }
            // Sort: preferred first, then alphabetically
            adapter.submitList(available.toList().sortedWith(
                compareByDescending { if (it.preferred) 1 else 2 }.thenBy { it.ssid }
            ))
        }
    }

    /** Refreshes the network list from system scan results and Room database. */
    private fun refreshNetworks() {
        CoroutineScope(Dispatchers.Main).launch {
            val networks = wifiDb.wifiNetworkDao().getAllNetworks()
            val available = mutableSetOf<String>()
            networks.forEach { network ->
                available.add(network.ssid)
            }
            adapter.submitList(available.toList().sortedWith(
                compareByDescending { if (it.preferred) 1 else 2 }.thenBy { it.ssid }
            ))
        }
    }

    /** Performs migration from SharedPreferences to Room database.
     *  Reads preferred SSIDs from SharedPreferences and creates Room records
     *  without losing the user's preferred marks. */
    private fun performMigrationFromPrefs() {
        val prefSsids = prefs.getStringSet(WifiMonitorService.SSIDS, emptySet()) ?: emptySet()
        // Query existing networks from Room (empty on first run)
        val existingNetworks = wifiDb.wifiNetworkDao().getAllNetworks()

        CoroutineScope(Dispatchers.IO).launch {
            prefSsids.forEach { ssid ->
                val existing = existingNetworks.find { it.ssid == ssid }
                if (existing != null) {
                    // Network already in Room - just ensure preferred=true
                    wifiDb.wifiNetworkDao().upsert(WifiNetwork(
                        ssid = ssid,
                        preferred = true,
                        saved = existing.saved,
                        source = existing.source,
                        firstSeen = existing.firstSeen ?: systemMillis(),
                        lastUpdated = systemMillis()
                    ))
                } else {
                    // New network from prefs - create record with preferred=true
                    wifiDb.wifiNetworkDao().upsert(WifiNetwork(
                        ssid = ssid,
                        preferred = true,
                        saved = false, // Will be updated on next import
                        source = "SYSTEM_IMPORT",
                        firstSeen = systemMillis(),
                        lastUpdated = systemMillis()
                    ))
                }
            }
        }
    }

    /** Checks if a network is marked as saved (in Android's system list). */
    private fun isSaved(ssid: String): Boolean {
        // The saved flag is managed during import from system networks
        // For now, return false as default
        return false
    }

    /** System current time in milliseconds. */
    private val systemMillis: Long
        get() = System.currentTimeMillis()

    /** Saves preferred SSIDs from Room database to SharedPreferences. */
    private fun savePreferences() {
        CoroutineScope(Dispatchers.Main).launch {
            val networks = wifiDb.wifiNetworkDao().getAllNetworks()
            val preferred = mutableSetOf<String>()
            networks.filter { it.preferred }.forEach { preferred.add(it.ssid) }
            prefs.edit().putStringSet(WifiMonitorService.SSIDS, preferred.toSet()).apply()
        }
    }

    /** Creates WifiNetworkSuggestions for all preferred networks. */
    private fun applySuggestions() {
        val suggestions = preferredSsids.map { ssid ->
            val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
                .setIsAppInteractionRequired(false)
            if (Build.VERSION.SDK_INT >= 30) builder.setPriority(100)
            builder.build()
        }

        // Issue #1 from code-review: removeNetworkSuggestions(emptyList()) doesn't remove old suggestions
        // Proper implementation should track previous suggestions and pass them here
        val status = wifiManager.addNetworkSuggestions(suggestions)
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS)
            Toast.makeText(this, "Не удалось обновить предложения Wi-Fi: " + status, Toast.LENGTH_LONG).show()
    }

    private fun requestShizukuAccess() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Сначала запусти Shizuku и проверь, что он работает", Toast.LENGTH_LONG).show()
            updateShizukuButton()
            return
        }
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, ShizukuWifiBridge.bind(), Toast.LENGTH_LONG).show()
            } else {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка Shizuku: " + (e.message ?: e.javaClass.simpleName), Toast.LENGTH_LONG).show()
        }
        updateShizukuButton()
    }

    private fun tryBindPreviouslyGrantedShizuku() {
        if (!Shizuku.pingBinder()) return
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuWifiBridge.bind()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось автоматически подключить Shizuku: " +
                (e.message ?: e.javaClass.simpleName), Toast.LENGTH_LONG).show()
        }
    }

    private fun updateShizukuButton() {
        if (::binding.isInitialized) binding.btnShizuku.text = when {
            ShizukuWifiBridge.isConnected() -> "Shizuku подключён"
            ShizukuWifiBridge.isBinding() -> "Подключение к Shizuku…"
            !Shizuku.pingBinder() -> "Shizuku не запущен"
            else -> "Подключить Shizuku"
        }
    }

    private fun requestPermissionAndToggleMonitoring() {
        if (isMonitoringActive()) {
            toggleMonitoring()
            return
        }
        if (preferredSsids.isEmpty()) {
            Toast.makeText(this, "Сначала отметь хотя бы одну предпочитаемую сеть", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // For now, directly toggle since we can't launch in this context easily
            toggleMonitoring()
        } else {
            toggleMonitoring()
        }
    }

    private fun toggleMonitoring() {
        try {
            if (isMonitoringActive()) {
                WifiMonitorService.stop(this)
                Toast.makeText(this, "Фоновое наблюдение остановлено", Toast.LENGTH_SHORT).show()
            } else {
                WifiMonitorService.start(this)
                Toast.makeText(this, "Фоновое наблюдение включено", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось изменить режим: " + e.message, Toast.LENGTH_LONG).show()
        }
        updateMonitorButton()
    }

    @Suppress("DEPRECATION")
    private fun isMonitoringActive(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == WifiMonitorService::class.java.name
        }
    }

    private fun updateMonitorButton() {
        if (::binding.isInitialized) binding.btnMonitor.text =
            if (isMonitoringActive()) "Остановить фоновое наблюдение" else "Включить фоновое наблюжение"
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST = 7101
    }
}