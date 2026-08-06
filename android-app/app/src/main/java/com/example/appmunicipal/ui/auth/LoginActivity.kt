package com.example.appmunicipal.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityLoginBinding
import com.example.appmunicipal.ui.admin.AdminDashboardActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDashboardActivity
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
        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonLogin.setOnClickListener { login() }
    }

    private fun login() {
        val email = binding.editEmail.text?.toString()?.trim().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        binding.layoutEmail.error = if (email.isBlank()) "Ingrese el correo." else null
        binding.layoutPassword.error = if (password.isBlank()) "Ingrese la contraseña." else null
        if (email.isBlank() || password.isBlank()) return

        binding.progressLogin.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.login(email, password) }
                .onSuccess { response ->
                    tokenStore.guardarSesion(response.token, response.name, response.role)
                    val target = if (response.role == "ADMINISTRADOR") {
                        AdminDashboardActivity::class.java
                    } else TecnicoDashboardActivity::class.java
                    startActivity(Intent(this@LoginActivity, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                }
                .onFailure { error ->
                    Snackbar.make(binding.root, error.message ?: "No fue posible iniciar sesión.", Snackbar.LENGTH_LONG).show()
                }
            binding.progressLogin.visibility = View.GONE
            binding.buttonLogin.isEnabled = true
        }
    }
}
