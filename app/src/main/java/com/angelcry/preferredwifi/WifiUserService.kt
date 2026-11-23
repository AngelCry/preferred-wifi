package com.angelcry.preferredwifi

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Process
import androidx.annotation.Keep
import java.lang.reflect.Method

/** Privileged helper running in Shizuku's shell/root UserService process. */
class WifiUserService @Keep constructor(private val context: Context) : IWifiControl.Stub() {

    override fun getProcessUid(): Int = Process.myUid()

    override fun connectToSavedNetwork(ssid: String): String {
        if (ssid.isBlank()) return "ERROR: empty SSID"
        return try {
            // Shizuku's ADB backend runs as UID 2000. Use the shell package
            // attribution so WifiService sees a package belonging to that UID.
            val serviceContext = if (Process.myUid() == 2000) {
                context.createPackageContext("com.android.shell", Context.CONTEXT_RESTRICTED)
            } else {
                context
            }
            val wifi = serviceContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val configuredMethod = wifi.javaClass.getDeclaredMethod("getConfiguredNetworks")
            configuredMethod.isAccessible = true
            val configured = configuredMethod.invoke(wifi) as? List<*>
                ?: return "ERROR: saved networks are unavailable"

            val match = configured.firstOrNull { item ->
                item != null && runCatching {
                    val field = item.javaClass.getField("SSID")
                    (field.get(item) as? String)?.trim('"') == ssid
                }.getOrDefault(false)
            } ?: return "ERROR: network is not saved on this device"

            val networkId = match.javaClass.getField("networkId").getInt(match)
            if (networkId < 0) return "ERROR: invalid saved network id"

            val listenerClass = Class.forName("android.net.wifi.WifiManager\$ActionListener")
            val connectMethod: Method = wifi.javaClass.getDeclaredMethod(
                "connect", Int::class.javaPrimitiveType, listenerClass
            )
            connectMethod.isAccessible = true
            connectMethod.invoke(wifi, networkId, null)
            "OK: requested connection to " + ssid + " (networkId=" + networkId +
                ", uid=" + Process.myUid() + ")"
        } catch (e: Throwable) {
            val cause = e.cause ?: e
            "ERROR: " + cause.javaClass.simpleName + ": " +
                (cause.message ?: "Wi-Fi control failed")
        }
    }

    @Keep
    fun destroy() {
        // Lifecycle management is handled by Shizuku UserService, not by the application.
        // Removed System.exit(0) which would terminate the entire Shizuku process.
    }
}