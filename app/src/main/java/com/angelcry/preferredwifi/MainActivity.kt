package com.angelcry.preferredwifi

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.angelcry.preferredwifi.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var adapter: NetworkAdapter
    private val preferredSsids = mutableSetOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) loadNetworks()
        else Toast.makeText(this, "Нужно разрешение на доступ к Wi-Fi сетям", Toast.LENGTH_LONG).show()
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || Build.VERSION.SDK_INT < 33) toggleMonitoring()
        else Toast.makeText(this, "Для фонового наблюдения разреши уведомления", Toast.LENGTH_LONG).show()
    }

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, ShizukuWifiBridge.bind(), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Разрешение Shizuku не выдано", Toast.LENGTH_LONG).show()
            }
            updateShizukuButton()
        }
    }
    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { updateShizukuButton() }
    }
    private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread { updateShizukuButton() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        prefs = getSharedPreferences(WifiMonitorService.PREFS, Context.MODE_PRIVATE)
        preferredSsids.addAll(prefs.getStringSet(WifiMonitorService.SSIDS, emptySet()) ?: emptySet())

        adapter = NetworkAdapter(preferredSsids) { ssid, checked ->
            if (checked) preferredSsids.add(ssid) else preferredSsids.remove(ssid)
            savePreferences()
            applySuggestions()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.btnRefresh.setOnClickListener { checkPermissionsAndLoad() }
        binding.btnApply.setOnClickListener {
            savePreferences()
            applySuggestions()
            Toast.makeText(this, "Предпочтения переданы Android", Toast.LENGTH_SHORT).show()
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
    }

    override fun onResume() {
        super.onResume()
        updateMonitorButton()
        updateShizukuButton()
    }

    override fun onDestroy() {
        ShizukuWifiBridge.setStateListener(null)
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeBinderDeadListener(shizukuDeadListener)
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        super.onDestroy()
    }

    private fun checkPermissionsAndLoad() {
        val required = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) required.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissionLauncher.launch(missing.toTypedArray())
        else loadNetworks()
    }

    private fun loadNetworks() {
        val available = linkedSetOf<String>()
        try {
            wifiManager.scanResults.forEach {
                val ssid = it.SSID?.trim('"').orEmpty()
                if (ssid.isNotBlank()) available.add(ssid)
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, "Нет доступа к результатам Wi-Fi сканирования", Toast.LENGTH_LONG).show()
        }
        available.addAll(preferredSsids)
        adapter.submitList(available.sortedWith(
            compareByDescending<String> { preferredSsids.contains(it) }.thenBy { it }))
    }

    private fun savePreferences() {
        prefs.edit().putStringSet(WifiMonitorService.SSIDS, preferredSsids.toSet()).apply()
    }

    private fun applySuggestions() {
        // Determine which suggestions are stale (no longer preferred)
        val staleSuggestions = if (Build.VERSION.SDK_INT >= 30) {
            // Primary path: get actual suggestions from system, filter to stale only
            wifiManager.getNetworkSuggestions().filter { suggestion ->
                val ssid = suggestion.ssid
                ssid != null && ssid !in preferredSsids
            }
        } else {
            // Fallback: build from SharedPreferences tracked SSIDs (no setPriority - API 30+)
            val previousSsids = prefs.getStringSet(PREFS_ADDED_SUGGESTIONS, emptySet()) ?: emptySet()
            previousSsids.filter { it !in preferredSsids }.map { ssid ->
                WifiNetworkSuggestion.Builder().setSsid(ssid)
                    .setIsAppInteractionRequired(false)
                    .build()
            }
        }

        // Remove only stale suggestions using proper List<WifiNetworkSuggestion> (not Array<String>)
        if (staleSuggestions.isNotEmpty()) {
            wifiManager.removeNetworkSuggestions(staleSuggestions)
        }

        val suggestions = preferredSsids.map { ssid ->
            val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
                .setIsAppInteractionRequired(false)
            if (Build.VERSION.SDK_INT >= 30) builder.setPriority(100)
            builder.build()
        }

        val status = wifiManager.addNetworkSuggestions(suggestions)
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS)
            Toast.makeText(this, "Не удалось обновить предложения Wi-Fi: " + status, Toast.LENGTH_LONG).show()

        // Update SharedPreferences tracking (fallback for API < 30)
        prefs.edit().putStringSet(PREFS_ADDED_SUGGESTIONS, preferredSsids.toSet()).apply()
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
        if (!::binding.isInitialized) return
        binding.btnShizuku.text = when {
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
        ) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else toggleMonitoring()
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
            if (isMonitoringActive()) "Остановить фоновое наблюдение" else "Включить фоновое наблюдение"
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST = 7101
        private const val PREFS_ADDED_SUGGESTIONS = "added_suggestions"
    }
}
