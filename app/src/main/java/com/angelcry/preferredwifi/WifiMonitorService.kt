package com.angelcry.preferredwifi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Passively observes scans initiated by Android or other apps; never polls startScan().
 *  Binder IPC (Shizuku connect) is moved off the main thread onto a HandlerThread. */
class WifiMonitorService : Service() {
    private lateinit var wifiManager: WifiManager
    private var lastAnnouncedNetwork: String? = null
    private var lastAnnounceWasConnected: Boolean = false

    // Background HandlerThread for Shizuku Binder IPC operations
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    // Main thread Handler for showing notifications
    private lateinit var mainHandler: Handler

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION &&
                intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                inspectScanResults()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Create HandlerThread for Binder IPC; start() MUST be called before looper/Handler
        backgroundThread = HandlerThread("wifi-monitor-binder")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)

        // Main thread handler for UI/notifications
        mainHandler = Handler(Looper.getMainLooper())

        createNotificationChannels()
        ContextCompat.registerReceiver(this, scanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= 34)
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, ONGOING_ID, ongoingNotification(), type)
        running = true
        return START_STICKY
    }

    private fun inspectScanResults() {
        val preferred = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(SSIDS, emptySet()).orEmpty()
        if (preferred.isEmpty()) return
        try {
            val best = wifiManager.scanResults
                .filter { it.SSID != null && preferred.contains(it.SSID.trim('"')) }
                .sortedWith(compareByDescending<android.net.wifi.ScanResult> { it.frequency }
                    .thenByDescending { it.level }).firstOrNull() ?: return

            val ssid = best.SSID.trim('"')
            val band = when (best.frequency) {
                in 2400..2500 -> "2,4 ГГц"
                in 4900..5900 -> "5 ГГц"
                in 5925..7125 -> "6 ГГц"
                else -> best.frequency.toString() + " МГц"
            }
            val key = ssid + "|" + best.frequency
            val currentInfo = wifiManager.connectionInfo
            val currentSsid = currentInfo?.ssid?.trim('"')
            val currentFrequency = currentInfo?.frequency ?: 0
            if (currentSsid == ssid && currentFrequency >= best.frequency) {
                lastAnnouncedNetwork = key
                lastAnnounceWasConnected = true
                return
            }
            // If Shizuku becomes available after the network was first seen,
            // allow the next system scan to retry the connection request.
            // Suppress only if: already announced AND (was connected before OR still disconnected).
            // If bridge was disconnected at announcement but is now connected → retry connect.
            if (key == lastAnnouncedNetwork &&
                (lastAnnounceWasConnected || !ShizukuWifiBridge.isConnected())) {
                return
            }
            lastAnnouncedNetwork = key
            lastAnnounceWasConnected = ShizukuWifiBridge.isConnected()

            // Binder IPC moved to background HandlerThread
            backgroundHandler.post {
                val actionResult = try {
                    if (ShizukuWifiBridge.isConnected()) {
                        ShizukuWifiBridge.connectToSavedNetwork(ssid)
                    } else {
                        "Shizuku не подключён: автоматическое переключение недоступно"
                    }
                } catch (e: Exception) {
                    "Ошибка связи с Shizuku: ${e.message ?: "binder unavailable"}"
                }
                // Post notification back to main thread
                mainHandler.post {
                    val message = ssid + " (" + band + ", " + best.level + " dBm). " + actionResult
                    val title = if (actionResult.startsWith("OK:"))
                        "Запрошено переключение Wi-Fi" else "Обнаружена предпочитаемая сеть"
                    val notification = NotificationCompat.Builder(this, DETECTION_CHANNEL)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(ssid + " · " + band + " · " + actionResult)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                        .setAutoCancel(true).build()
                    getSystemService(NotificationManager::class.java).notify(DETECTION_ID, notification)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Wi-Fi scan results are not accessible", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect scan results", e)
        }
    }

    private fun ongoingNotification(): Notification {
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, MONITOR_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Preferred Wi-Fi: наблюдение включено")
            .setContentText("Ожидание системных результатов сканирования")
            .setContentIntent(pending).setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                MONITOR_CHANNEL, "Фоновое наблюдение", NotificationManager.IMPORTANCE_LOW))
            manager.createNotificationChannel(NotificationChannel(
                DETECTION_CHANNEL, "Обнаружение сетей", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(scanReceiver) } catch (_: IllegalArgumentException) { }
        running = false
        backgroundThread.quitSafely()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PREFS = "preferred_wifi"
        const val SSIDS = "ssids"
        private const val TAG = "WifiMonitorService"
        private const val MONITOR_CHANNEL = "wifi_monitor"
        private const val DETECTION_CHANNEL = "wifi_detection"
        private const val ONGOING_ID = 4101
        private const val DETECTION_ID = 4102

        @Volatile private var running = false
        fun isRunning(): Boolean = running

        fun start(context: Context) = ContextCompat.startForegroundService(
            context, Intent(context, WifiMonitorService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, WifiMonitorService::class.java))
    }
}