package com.example.appmunicipal.ui.tecnico

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.R
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
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)

        binding.toolbar.subtitle = getString(R.string.dashboard_sesion, tokenStore.nombre().orEmpty())
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.accionCerrarSesion) {
                cerrarSesion()
                true
            } else {
                false
            }
        }

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

        binding.swipeRefresh.setOnRefreshListener {
            cargarResumen()
            NotificationScheduler.sincronizarAhora(this)
        }

        solicitarPermisoNotificaciones()
        NotificationScheduler.programar(this)
    }

    override fun onPersonalSessionResume() {
        cargarResumen()
        NotificationScheduler.sincronizarAhora(this)
    }

    private fun cargarResumen() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            runCatching { repository.listarTecnico() }
                .onSuccess { reportes ->
                    val activos = reportes.filter { it.status in setOf("ASIGNADO", "EN_PROCESO", "PAUSADO") }
                    val cerrados = reportes.count { it.status == "CERRADO" }
                    val pausados = activos.count { it.status == "PAUSADO" }
                    binding.textTicketsAsignados.text = activos.size.toString()
                    binding.textTicketsResumen.text = when {
                        activos.isEmpty() -> getString(R.string.dashboard_sin_pendientes)
                        pausados > 0 -> getString(R.string.dashboard_activos_en_pausa, activos.size, pausados)
                        else -> getString(R.string.dashboard_listos, activos.size)
                    }
                    binding.textHistorialCantidad.text = cerrados.toString()
                    binding.textHistorialResumen.text =
                        resources.getQuantityString(R.plurals.trabajos_completados, cerrados, cerrados)
                }
                .onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: getString(R.string.dashboard_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.swipeRefresh.isRefreshing = false
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
