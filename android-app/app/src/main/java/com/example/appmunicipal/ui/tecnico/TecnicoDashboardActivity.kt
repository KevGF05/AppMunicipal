package com.example.appmunicipal.ui.tecnico

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
import com.example.appmunicipal.databinding.ActivityTecnicoDashboardBinding
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.auth.CambiarPasswordActivity
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.ui.publico.RegistroActividadActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TecnicoDashboardActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityTecnicoDashboardBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }

    private val permisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (tokenStore.passwordChangeRequired()) {
            startActivity(Intent(this, CambiarPasswordActivity::class.java))
            finish()
            return
        }

        binding = ActivityTecnicoDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()
        binding.textBienvenida.text = "Sesión: ${tokenStore.nombre()}"

        binding.cardTicketsAsignados.setOnClickListener {
            startActivity(Intent(this, TecnicoTicketsActivity::class.java))
        }
        binding.cardHistorial.setOnClickListener {
            startActivity(Intent(this, TecnicoHistorialActivity::class.java))
        }
        binding.cardCrearReporte.setOnClickListener {
            startActivity(Intent(this, RegistroActividadActivity::class.java).apply {
                putExtra(RegistroActividadActivity.EXTRA_MODO_TECNICO, true)
            })
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
            runCatching { repository.listarTecnico() }
                .onSuccess { reportes ->
                    val activos = reportes.filter { it.status in setOf("ASIGNADO", "EN_PROCESO", "PAUSADO") }
                    val cerrados = reportes.count { it.status == "CERRADO" }
                    val pausados = activos.count { it.status == "PAUSADO" }
                    binding.textTicketsAsignados.text = activos.size.toString()
                    binding.textTicketsResumen.text = when {
                        activos.isEmpty() -> "No tiene trabajos pendientes en este momento."
                        pausados == 1 -> "${activos.size} activos · 1 en pausa"
                        pausados > 1 -> "${activos.size} activos · $pausados en pausa"
                        else -> "${activos.size} trabajos listos para atender o en proceso."
                    }
                    binding.textHistorialCantidad.text = cerrados.toString()
                    binding.textHistorialResumen.text = if (cerrados == 1) "1 trabajo completado." else "$cerrados trabajos completados."
                }
                .onFailure { error ->
                    Snackbar.make(binding.root, error.message ?: "No fue posible actualizar el dashboard.", Snackbar.LENGTH_LONG).show()
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
