package com.angelcry.preferredwifi

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import rikka.shizuku.Shizuku

/** Binds the privileged Wi-Fi helper after explicit user permission. */
object ShizukuWifiBridge {
    private const val BIND_TIMEOUT_MS = 15_000L

    @Volatile private var remote: IWifiControl? = null
    @Volatile private var binding = false
    @Volatile private var stateListener: ((String) -> Unit)? = null
    private var connection: ServiceConnection? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null

    private val args = Shizuku.UserServiceArgs(
        ComponentName("com.angelcry.preferredwifi", WifiUserService::class.java.name)
    ).processNameSuffix("wifi-control").debuggable(BuildConfig.DEBUG).version(1).daemon(false)

    fun setStateListener(listener: ((String) -> Unit)?) {
        stateListener = listener
    }

    private fun reportState(message: String) {
        mainHandler.post {
            stateListener?.invoke(message)
        }
    }

    @Synchronized
    fun bind(): String {
        if (!Shizuku.pingBinder()) return "Shizuku не запущен"
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "Нет разрешения Shizuku"
        }
        if (remote != null) return "Shizuku подключён"
        if (binding) return "Подключение к Shizuku уже выполняется"

        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service == null) {
                    failBinding("Shizuku запустил UserService без Binder")
                    return
                }
                try {
                    val control = IWifiControl.Stub.asInterface(service)
                        ?: throw IllegalStateException("Не удалось получить IWifiControl")
                    val uid = control.getProcessUid()
                    remote = control
                    synchronized(this@ShizukuWifiBridge) {
                        binding = false
                        timeout?.let(mainHandler::removeCallbacks)
                        timeout = null
                    }
                    reportState("Shizuku UserService подключён (UID=$uid)")
                } catch (e: Exception) {
                    failBinding("Ошибка проверки UserService: ${e.message ?: e.javaClass.simpleName}")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                synchronized(this@ShizukuWifiBridge) {
                    remote = null
                    binding = false
                    timeout?.let(mainHandler::removeCallbacks)
                    timeout = null
                    connection = null
                }
                reportState("Соединение с Shizuku UserService потеряно")
            }

            override fun onBindingDied(name: ComponentName?) {
                failBinding("Shizuku сообщил о завершении привязки UserService")
            }

            override fun onNullBinding(name: ComponentName?) {
                failBinding("Shizuku вернул пустое подключение UserService")
            }
        }

        connection = serviceConnection
        binding = true
        return try {
            Shizuku.bindUserService(args, serviceConnection)
            val timeoutTask = Runnable {
                val shouldFail = synchronized(this) { binding && remote == null }
                if (shouldFail) {
                    failBinding("Тайм-аут: UserService не подключился за 15 секунд")
                }
            }
            timeout = timeoutTask
            mainHandler.postDelayed(timeoutTask, BIND_TIMEOUT_MS)
            "Запрошено подключение к Shizuku UserService…"
        } catch (e: Exception) {
            binding = false
            connection = null
            "Ошибка запуска UserService: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    @Synchronized
    private fun failBinding(message: String) {
        val current = connection
        binding = false
        remote = null
        connection = null
        timeout?.let(mainHandler::removeCallbacks)
        timeout = null
        if (current != null) {
            try {
                Shizuku.unbindUserService(args, current, true)
            } catch (_: Exception) {
                // Keep the original startup/binding error for the user.
            }
        }
        reportState(message)
    }

    @Synchronized
    fun isConnected(): Boolean = remote != null

    @Synchronized
    fun isBinding(): Boolean = binding

    fun connectToSavedNetwork(ssid: String): String {
        val service = remote ?: return "Shizuku UserService не подключён"
        return try {
            service.connectToSavedNetwork(ssid)
        } catch (e: RemoteException) {
            synchronized(this) {
                remote = null
                binding = false
            }
            reportState("Ошибка связи с Shizuku: ${e.message ?: "binder unavailable"}")
            "Ошибка связи с Shizuku: " + (e.message ?: "binder unavailable")
        }
    }

    @Synchronized
    fun unbind() {
        val current = connection ?: return
        timeout?.let(mainHandler::removeCallbacks)
        timeout = null
        try { Shizuku.unbindUserService(args, current, true) } catch (_: Exception) { }
        connection = null
        remote = null
        binding = false
    }
}
