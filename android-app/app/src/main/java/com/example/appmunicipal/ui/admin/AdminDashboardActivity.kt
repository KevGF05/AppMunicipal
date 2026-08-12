package com.example.appmunicipal.ui.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminDashboardBinding
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminDashboardActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }

    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        binding.textBienvenida.text = "Sesión: ${tokenStore.nombre()}"

        binding.cardTickets.setOnClickListener {
            startActivity(Intent(this, AdminTicketsActivity::class.java))
        }
        binding.cardTecnicos.setOnClickListener {
            startActivity(Intent(this, AdminTecnicosActivity::class.java))
        }
        binding.cardTiposReporte.setOnClickListener {
            startActivity(Intent(this, AdminReportTypesActivity::class.java))
        }
        binding.buttonActualizarDashboard.setOnClickListener {
            cargarResumen()
            NotificationScheduler.sincronizarAhora(this)
        }
        binding.buttonCerrarSesion.setOnClickListener { cerrarSesion() }

        solicitarPermisoNotificaciones()
        NotificationScheduler.programar(this)
    }

    override fun onPersonalSessionResume() {
        cargarResumen()
        NotificationScheduler.sincronizarAhora(this)
    }

    private fun cargarResumen() {
        binding.progressDashboard.visibility = View.VISIBLE
        binding.buttonActualizarDashboard.isEnabled = false

        lifecycleScope.launch {
            runCatching { repository.listarAdmin(null) }
                .onSuccess { reportes ->
                    val nuevos = reportes.count { it.status == "CREADO" }
                    val activos = reportes.count {
                        it.status == "APROBADO" || it.status == "ASIGNADO" || it.status == "EN_PROCESO" || it.status == "PAUSADO"
                    }
                    binding.textTicketsNuevos.text = nuevos.toString()
                    binding.textTicketsNuevosEtiqueta.text = if (nuevos == 1) "ticket nuevo" else "tickets nuevos"
                    binding.textTicketsResumen.text = when {
                        activos == 0 -> "No hay trabajos activos en este momento."
                        activos == 1 -> "1 trabajo está pendiente o en ejecución."
                        else -> "$activos trabajos están pendientes o en ejecución."
                    }
                }
                .onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: "No fue posible actualizar el dashboard.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.progressDashboard.visibility = View.GONE
            binding.buttonActualizarDashboard.isEnabled = true
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun cerrarSesion() {
        NotificationScheduler.cancelar(this)
        tokenStore.limpiar()
        startActivity(
            Intent(this, InicioActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}
