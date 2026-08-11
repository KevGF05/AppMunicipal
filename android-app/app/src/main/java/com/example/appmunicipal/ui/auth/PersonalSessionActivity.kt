package com.example.appmunicipal.ui.auth

import android.content.Intent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.notifications.NotificationScheduler

/**
 * Base para pantallas exclusivas de administrador/técnico.
 *
 * La sesión local expira tras 30 minutos sin interacción del usuario. Se valida al
 * regresar a la pantalla y antes de procesar un nuevo toque, para evitar que una
 * pantalla protegida siga utilizándose después del periodo de inactividad.
 */
abstract class PersonalSessionActivity : AppCompatActivity() {
    private val sessionStore by lazy { TokenStore(this) }
    private var redirectingToLogin = false

    final override fun onResume() {
        super.onResume()
        if (!validarSesion()) return
        sessionStore.marcarActividad()
        onPersonalSessionResume()
    }

    protected open fun onPersonalSessionResume() = Unit

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (!validarSesion()) return true
            sessionStore.marcarActividad()
        }
        return super.dispatchTouchEvent(event)
    }

    private fun validarSesion(): Boolean {
        if (redirectingToLogin) return false
        if (sessionStore.sesionActiva()) return true

        redirectingToLogin = true
        NotificationScheduler.cancelar(this)
        sessionStore.limpiar()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
        return false
    }
}
