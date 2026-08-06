package com.example.appmunicipal.data.local

import android.content.Context

class TokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("sesion_municipal", Context.MODE_PRIVATE)

    fun guardarSesion(token: String, nombre: String, rol: String) {
        preferences.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROL, rol)
            .apply()
    }

    fun token(): String? = preferences.getString(KEY_TOKEN, null)
    fun nombre(): String = preferences.getString(KEY_NOMBRE, "").orEmpty()
    fun rol(): String = preferences.getString(KEY_ROL, "").orEmpty()

    fun limpiar() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_ROL = "rol"
    }
}
