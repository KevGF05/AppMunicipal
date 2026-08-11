package com.example.appmunicipal.data.local

import android.content.Context

class TokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("sesion_municipal", Context.MODE_PRIVATE)

    fun guardarSesion(token: String, nombre: String, rol: String, passwordChangeRequired: Boolean) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROL, rol)
            .putBoolean(KEY_PASSWORD_CHANGE_REQUIRED, passwordChangeRequired)
            .putLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis())
            .apply()
    }

    fun token(): String? = preferences.getString(KEY_TOKEN, null)
    fun nombre(): String = preferences.getString(KEY_NOMBRE, "").orEmpty()
    fun rol(): String = preferences.getString(KEY_ROL, "").orEmpty()
    fun passwordChangeRequired(): Boolean = preferences.getBoolean(KEY_PASSWORD_CHANGE_REQUIRED, false)

    fun marcarActividad() {
        if (!token().isNullOrBlank()) {
            preferences.edit().putLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis()).apply()
        }
    }

    fun sesionExpirada(): Boolean {
        if (token().isNullOrBlank()) return true
        val ultimaActividad = preferences.getLong(KEY_LAST_ACTIVITY_AT, 0L)
        if (ultimaActividad <= 0L) return true
        return System.currentTimeMillis() - ultimaActividad >= SESSION_IDLE_TIMEOUT_MS
    }

    fun sesionActiva(): Boolean = !token().isNullOrBlank() && !sesionExpirada()

    fun marcarPasswordActualizada() {
        preferences.edit()
            .putBoolean(KEY_PASSWORD_CHANGE_REQUIRED, false)
            .putLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis())
            .apply()
    }

    fun limpiar() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val SESSION_IDLE_TIMEOUT_MINUTES = 30L
        private const val SESSION_IDLE_TIMEOUT_MS = SESSION_IDLE_TIMEOUT_MINUTES * 60L * 1000L
        private const val KEY_TOKEN = "token"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_ROL = "rol"
        private const val KEY_PASSWORD_CHANGE_REQUIRED = "password_change_required"
        private const val KEY_LAST_ACTIVITY_AT = "last_activity_at"
    }
}
