package com.example.appmunicipal.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val UNIQUE_WORK = "municipal_ticket_notifications"
    private const val UNIQUE_NOW = "municipal_ticket_notifications_now"

    fun programar(context: Context) {
        ForegroundNotificationMonitor.iniciar(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        sincronizarAhora(context)
    }

    fun sincronizarAhora(context: Context) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_NOW,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelar(context: Context) {
        ForegroundNotificationMonitor.detener()
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK)
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_NOW)
    }
}
