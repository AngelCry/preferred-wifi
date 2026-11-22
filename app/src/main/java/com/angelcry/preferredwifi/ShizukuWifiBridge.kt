package com.angelcry.preferredwifi

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import rikka.shizuku.Shizuku

/** Binds the privileged Wi-Fi helper after explicit user permission. */
object ShizukuWifiBridge {
    @Volatile private var remote: IWifiControl? = null
    @Volatile private var binding = false
    private var connection: ServiceConnection? = null

    private val args = Shizuku.UserServiceArgs(
        ComponentName("com.angelcry.preferredwifi", WifiUserService::class.java.name)
    ).processNameSuffix("wifi-control").debuggable(BuildConfig.DEBUG).version(1).daemon(false)

    @Synchronized
    fun bind(): String {
        if (!Shizuku.pingBinder()) return "Shizuku не запущен"
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "Нет разрешения Shizuku"
        }
        if (remote != null) return "Shizuku подключён"
        if (binding) return "Подключение к Shizuku…"

        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                remote = IWifiControl.Stub.asInterface(service)
                binding = false
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                remote = null
                binding = false
            }
        }
        connection = serviceConnection
        binding = true
        return try {
            Shizuku.bindUserService(args, serviceConnection)
            "Запрошено подключение Shizuku…"
        } catch (e: Exception) {
            binding = false
            connection = null
            "Ошибка Shizuku: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    fun isConnected(): Boolean = remote != null

    fun connectToSavedNetwork(ssid: String): String {
        val service = remote ?: return "Shizuku UserService не подключён"
        return try {
            service.connectToSavedNetwork(ssid)
        } catch (e: RemoteException) {
            remote = null
            "Ошибка связи с Shizuku: " + (e.message ?: "binder unavailable")
        }
    }

    @Synchronized
    fun unbind() {
        val current = connection ?: return
        try { Shizuku.unbindUserService(args, current, true) } catch (_: Exception) { }
        connection = null
        remote = null
        binding = false
    }
}