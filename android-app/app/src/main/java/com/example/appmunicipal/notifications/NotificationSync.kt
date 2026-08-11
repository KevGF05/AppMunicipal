package com.example.appmunicipal.notifications

import android.content.Context
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository

/**
 * Sincroniza las notificaciones pendientes del usuario autenticado.
 *
 * Importante: esta operación NO actualiza la hora de actividad de la sesión.
 * Recibir o consultar notificaciones no mantiene al usuario autenticado; solo una
 * interacción real del usuario reinicia el contador de inactividad.
 */
object NotificationSync {
    suspend fun ejecutar(context: Context) {
        val appContext = context.applicationContext
        val store = TokenStore(appContext)
        if (!store.sesionActiva() || store.passwordChangeRequired()) return

        val repository = AppRepository(appContext)
        repository.notificacionesNoLeidas().forEach { item ->
            if (NotificationHelper.mostrar(appContext, item)) {
                repository.marcarNotificacionLeida(item.id)
            }
        }
    }
}
