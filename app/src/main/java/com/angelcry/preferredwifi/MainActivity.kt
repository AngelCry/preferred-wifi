package com.angelcry.preferredwifi

import android.Manifest
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var adapter: NetworkAdapter

    private val preferredSsids = mutableSetOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            loadNetworks()
        } else {
            Toast.makeText(this, "Нужны разрешения для работы с Wi-Fi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        prefs = getSharedPreferences("preferred_wifi", Context.MODE_PRIVATE)

        // Load previously saved preferred SSIDs
        preferredSsids.addAll(prefs.getStringSet("ssids", emptySet()) ?: emptySet())

        adapter = NetworkAdapter(preferredSsids) { ssid, isPreferred ->
            if (isPreferred) {
                preferredSsids.add(ssid)
            } else {
                preferredSsids.remove(ssid)
            }
            saveAndApplySuggestions()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnRefresh.setOnClickListener {
            checkPermissionsAndLoad()
        }

        binding.btnApply.setOnClickListener {
            saveAndApplySuggestions()
            Toast.makeText(this, "Предпочтения применены системе", Toast.LENGTH_SHORT).show()
        }

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            loadNetworks()
        }
    }

    private fun loadNetworks() {
        // On Android 10+ we can't freely get all saved networks with passwords.
        // We show currently available networks + previously preferred ones.
        val available = mutableListOf<String>()

        try {
            val scanResults = wifiManager.scanResults
            scanResults?.forEach { result ->
                val ssid = result.SSID?.trim('"') ?: ""
                if (ssid.isNotBlank() && !available.contains(ssid)) {
                    available.add(ssid)
                }
            }
        } catch (e: SecurityException) {
            // Permission issue
        }

        // Also include previously preferred SSIDs even if not currently visible
        preferredSsids.forEach { ssid ->
            if (!available.contains(ssid)) {
                available.add(ssid)
            }
        }

        // Sort: preferred first, then alphabetically
        available.sortWith(compareByDescending<String> { preferredSsids.contains(it) }.thenBy { it })

        adapter.submitList(available)
    }

    private fun saveAndApplySuggestions() {
        // Save to preferences
        prefs.edit().putStringSet("ssids", preferredSsids).apply()

        // Build suggestions for the system
        val suggestions = preferredSsids.map { ssid ->
            WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsAppInteractionRequired(false) // system can use it automatically
                .setPriority(100) // high priority (equivalent to RANKING_SCORE_HIGH)
                .build()
        }

        // Remove old suggestions from this app and add new ones
        val status = wifiManager.removeNetworkSuggestions(emptyList()) // clear previous from this app
        val addStatus = wifiManager.addNetworkSuggestions(suggestions)

        if (addStatus == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // success
        } else {
            Toast.makeText(this, "Ошибка при добавлении предложений: $addStatus", Toast.LENGTH_LONG).show()
        }
    }
}
