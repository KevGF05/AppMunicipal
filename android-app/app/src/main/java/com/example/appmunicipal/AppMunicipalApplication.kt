package com.example.appmunicipal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.appmunicipal.notifications.ForegroundNotificationMonitor

/**
 * Mantiene el monitor rápido de notificaciones únicamente mientras la aplicación
 * está en primer plano. Para segundo plano se conserva WorkManager.
 */
class AppMunicipalApplication : Application(), Application.ActivityLifecycleCallbacks {
    private var startedActivities = 0
    private var changingConfigurations = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0 && !changingConfigurations) {
            ForegroundNotificationMonitor.iniciar(this)
        }
        startedActivities++
    }

    override fun onActivityResumed(activity: Activity) {
        // También se intenta iniciar aquí para cubrir un login realizado con la app
        // ya abierta, cuando la sesión todavía no existía al iniciar el proceso.
        ForegroundNotificationMonitor.iniciar(this)
    }

    override fun onActivityStopped(activity: Activity) {
        changingConfigurations = activity.isChangingConfigurations
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0 && !changingConfigurations) {
            ForegroundNotificationMonitor.detener()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
