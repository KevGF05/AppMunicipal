package com.example.appmunicipal.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.databinding.ActivityInicioBinding
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.admin.AdminDashboardActivity
import com.example.appmunicipal.ui.auth.CambiarPasswordActivity
import com.example.appmunicipal.ui.auth.LoginActivity
import com.example.appmunicipal.ui.publico.MisReportesActivity
import com.example.appmunicipal.ui.publico.RegistroActividadActivity
import com.example.appmunicipal.ui.publico.SeguimientoActivity
import com.example.appmunicipal.ui.tecnico.TecnicoDashboardActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InicioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInicioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scrollInicio.applySafeSystemInsets(extraTopDp = 8)

        binding.buttonReportar.setOnClickListener {
            startActivity(Intent(this, RegistroActividadActivity::class.java))
        }
        // Nuevo acceso a la lista local de codigos enviados desde este telefono.
        binding.buttonMisReportes.setOnClickListener {
            startActivity(Intent(this, MisReportesActivity::class.java))
        }
        binding.buttonSeguimiento.setOnClickListener {
            startActivity(Intent(this, SeguimientoActivity::class.java))
        }
        binding.buttonPersonal.setOnClickListener { abrirPersonal() }
    }

    private fun abrirPersonal() {
        val store = TokenStore(this)
        if (store.token().isNullOrBlank()) {
            abrirLogin()
            return
        }

        if (store.sesionExpirada()) {
            NotificationScheduler.cancelar(this)
            store.limpiar()
            abrirLogin(sesionExpirada = true)
            return
        }

        val rolLegible = when (store.rol()) {
            "ADMINISTRADOR" -> getString(R.string.rol_administrador)
            "TECNICO" -> getString(R.string.rol_tecnico)
            else -> {
                store.limpiar()
                abrirLogin()
                return
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.inicio_sesion_activa)
            .setMessage("${store.nombre()}\n$rolLegible")
            .setPositiveButton(R.string.accion_continuar) { _, _ -> continuarSesion(store) }
            .setNegativeButton(R.string.inicio_otra_cuenta) { _, _ ->
                NotificationScheduler.cancelar(this)
                store.limpiar()
                abrirLogin()
            }
            .show()
    }

    private fun continuarSesion(store: TokenStore) {
        store.marcarActividad()
        val target = when {
            store.rol() == "TECNICO" && store.passwordChangeRequired() -> CambiarPasswordActivity::class.java
            store.rol() == "ADMINISTRADOR" -> AdminDashboardActivity::class.java
            store.rol() == "TECNICO" -> TecnicoDashboardActivity::class.java
            else -> LoginActivity::class.java
        }
        startActivity(Intent(this, target))
    }

    private fun abrirLogin(sesionExpirada: Boolean = false) {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, sesionExpirada)
        )
    }
}
