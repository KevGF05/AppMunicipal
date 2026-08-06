package com.example.appmunicipal.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.databinding.ActivityInicioBinding
import com.example.appmunicipal.ui.admin.AdminDashboardActivity
import com.example.appmunicipal.ui.auth.LoginActivity
import com.example.appmunicipal.ui.publico.RegistroActividadActivity
import com.example.appmunicipal.ui.publico.SeguimientoActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDashboardActivity

class InicioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInicioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonReportar.setOnClickListener {
            startActivity(Intent(this, RegistroActividadActivity::class.java))
        }
        binding.buttonSeguimiento.setOnClickListener {
            startActivity(Intent(this, SeguimientoActivity::class.java))
        }
        binding.buttonPersonal.setOnClickListener { abrirPersonal() }
    }

    private fun abrirPersonal() {
        val store = TokenStore(this)
        val target = when {
            store.token().isNullOrBlank() -> LoginActivity::class.java
            store.rol() == "ADMINISTRADOR" -> AdminDashboardActivity::class.java
            store.rol() == "TECNICO" -> TecnicoDashboardActivity::class.java
            else -> LoginActivity::class.java
        }
        startActivity(Intent(this, target))
    }
}
