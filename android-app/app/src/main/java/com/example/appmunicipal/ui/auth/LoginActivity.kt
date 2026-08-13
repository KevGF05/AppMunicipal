package com.example.appmunicipal.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityLoginBinding
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.admin.AdminDashboardActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDashboardActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        // La flecha de la barra y el gesto de retroceso hacen lo mismo: regresar al inicio
        // publico, no dejar la pantalla de login a medias en la pila.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = volverAlInicio()
        })

        binding.buttonLogin.setOnClickListener { login() }

        if (intent.getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            Snackbar.make(binding.root, R.string.login_sesion_expirada, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun volverAlInicio() {
        startActivity(
            Intent(this, InicioActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun login() {
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        binding.layoutEmail.error = if (email.isBlank()) getString(R.string.login_falta_correo) else null
        binding.layoutPassword.error = if (password.isBlank()) getString(R.string.login_falta_password) else null
        if (email.isBlank() || password.isBlank()) return

        binding.progressLogin.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.login(email, password) }
                .onSuccess { response ->
                    tokenStore.guardarSesion(
                        response.token,
                        response.name,
                        response.role,
                        response.passwordChangeRequired
                    )
                    val target = when {
                        response.role == "TECNICO" && response.passwordChangeRequired -> CambiarPasswordActivity::class.java
                        response.role == "ADMINISTRADOR" -> AdminDashboardActivity::class.java
                        else -> TecnicoDashboardActivity::class.java
                    }
                    if (!response.passwordChangeRequired) {
                        NotificationScheduler.programar(this@LoginActivity)
                    }
                    startActivity(Intent(this@LoginActivity, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                }
                .onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: getString(R.string.login_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.progressLogin.visibility = View.GONE
            binding.buttonLogin.isEnabled = true
        }
    }

    companion object {
        const val EXTRA_SESSION_EXPIRED = "session_expired"
    }
}
