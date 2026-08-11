package com.example.appmunicipal.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appmunicipal.data.local.TokenStore

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)
        if (!store.sesionActiva() || store.passwordChangeRequired()) return Result.success()

        return runCatching {
            NotificationSync.ejecutar(applicationContext)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.success() }
        )
    }
}
