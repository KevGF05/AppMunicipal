package com.example.appmunicipal.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.R
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
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)

        // Sustituye al override de onBackPressed(), que quedo obsoleto en Android 13.
        // La barra tampoco lleva flecha: la contrasena temporal debe cambiarse antes de seguir.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        binding.buttonGuardarPassword.setOnClickListener { guardar() }
        binding.buttonCerrarSesion.setOnClickListener {
            tokenStore.limpiar()
            NotificationScheduler.cancelar(this)
            volverAlInicio()
        }
    }

    private fun guardar() {
        val nueva = binding.editNuevaPassword.text?.toString().orEmpty()
        val confirmar = binding.editConfirmarPassword.text?.toString().orEmpty()
        binding.layoutNuevaPassword.error = null
        binding.layoutConfirmarPassword.error = null

        var valido = true
        if (nueva.length < 8) {
            binding.layoutNuevaPassword.error = getString(R.string.password_min_caracteres)
            valido = false
        }
        if (nueva.none { it.isLetter() } || nueva.none { it.isDigit() }) {
            binding.layoutNuevaPassword.error = getString(R.string.password_letra_numero)
            valido = false
        }
        if (confirmar != nueva) {
            binding.layoutConfirmarPassword.error = getString(R.string.password_no_coincide)
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
                    Snackbar.make(
                        binding.root,
                        error.message ?: getString(R.string.password_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.progressPassword.visibility = View.GONE
            binding.buttonGuardarPassword.isEnabled = true
        }
    }

    private fun volverAlInicio() {
        startActivity(
            Intent(this, InicioActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
