package com.example.appmunicipal.notifications

import android.content.Context
import com.example.appmunicipal.data.local.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Revisa notificaciones con mayor frecuencia mientras la aplicación está visible.
 *
 * El monitor respeta estrictamente la sesión de 30 minutos: consultar el backend no
 * cuenta como actividad del usuario y nunca extiende el tiempo de sesión.
 */
object ForegroundNotificationMonitor {
    private const val POLL_INTERVAL_MS = 30_000L

    private var job: Job? = null
    private var scope: CoroutineScope? = null

    @Synchronized
    fun iniciar(context: Context) {
        if (job?.isActive == true) return

        val appContext = context.applicationContext
        val store = TokenStore(appContext)
        if (!store.sesionActiva() || store.passwordChangeRequired()) return

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope
        job = newScope.launch {
            while (isActive) {
                val currentStore = TokenStore(appContext)
                if (!currentStore.sesionActiva() || currentStore.passwordChangeRequired()) break

                runCatching { NotificationSync.ejecutar(appContext) }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    fun detener() {
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
    }
}
