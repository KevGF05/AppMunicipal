package com.example.appmunicipal.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityCambiarPasswordBinding
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDashboardActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CambiarPasswordActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityCambiarPasswordBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (tokenStore.token().isNullOrBlank()) {
            volverAlInicio()
            return
        }
        binding = ActivityCambiarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()
        binding.buttonGuardarPassword.setOnClickListener { guardar() }
        binding.buttonCerrarSesion.setOnClickListener {
            tokenStore.limpiar()
            NotificationScheduler.cancelar(this)
            volverAlInicio()
        }
    }

    override fun onBackPressed() {
        // La contraseña temporal debe cambiarse antes de continuar.
    }

    private fun guardar() {
        val nueva = binding.editNuevaPassword.text?.toString().orEmpty()
        val confirmar = binding.editConfirmarPassword.text?.toString().orEmpty()
        binding.layoutNuevaPassword.error = null
        binding.layoutConfirmarPassword.error = null

        var valido = true
        if (nueva.length < 8) {
            binding.layoutNuevaPassword.error = "Use al menos 8 caracteres."
            valido = false
        }
        if (nueva.none { it.isLetter() } || nueva.none { it.isDigit() }) {
            binding.layoutNuevaPassword.error = "Incluya al menos una letra y un número."
            valido = false
        }
        if (confirmar != nueva) {
            binding.layoutConfirmarPassword.error = "Las contraseñas no coinciden."
            valido = false
        }
        if (!valido) return

        binding.progressPassword.visibility = View.VISIBLE
        binding.buttonGuardarPassword.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.cambiarPassword(nueva, confirmar) }
                .onSuccess {
                    tokenStore.marcarPasswordActualizada()
                    NotificationScheduler.programar(this@CambiarPasswordActivity)
                    startActivity(
                        Intent(this@CambiarPasswordActivity, TecnicoDashboardActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    finish()
                }
                .onFailure { error ->
                    Snackbar.make(binding.root, error.message ?: "No fue posible cambiar la contraseña.", Snackbar.LENGTH_LONG).show()
                }
            binding.progressPassword.visibility = View.GONE
            binding.buttonGuardarPassword.isEnabled = true
        }
    }

    private fun volverAlInicio() {
        startActivity(Intent(this, InicioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
