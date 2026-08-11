package com.example.appmunicipal.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.model.NotificationResponse
import com.example.appmunicipal.ui.admin.AdminDashboardActivity
import com.example.appmunicipal.ui.admin.AdminDetalleActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDashboardActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDetalleActivity

object NotificationHelper {
    private const val CHANNEL_ID = "tickets_municipales"

    fun mostrar(context: Context, item: NotificationResponse): Boolean {
        crearCanal(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        val role = TokenStore(context).rol()
        val intent = when (role) {
            "ADMINISTRADOR" -> item.reportId?.let {
                Intent(context, AdminDetalleActivity::class.java).putExtra(AdminDetalleActivity.EXTRA_ID, it)
            } ?: Intent(context, AdminDashboardActivity::class.java)
            "TECNICO" -> item.reportId?.let {
                Intent(context, TecnicoDetalleActivity::class.java).putExtra(TecnicoDetalleActivity.EXTRA_ID, it)
            } ?: Intent(context, TecnicoDashboardActivity::class.java)
            else -> return false
        }.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.title)
            .setContentText(item.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(item.id.toInt(), notification)
        return true
    }

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tickets municipales",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de nuevos reportes y tickets asignados"
        }
        manager.createNotificationChannel(channel)
    }
}
