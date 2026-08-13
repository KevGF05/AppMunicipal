package com.example.appmunicipal.ui.admin

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
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)

        // El nombre de la sesion pasa al subtitulo de la barra: antes era un TextView
        // suelto que empujaba las tarjetas hacia abajo.
        binding.toolbar.subtitle = getString(R.string.dashboard_sesion, tokenStore.nombre().orEmpty())
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.accionCerrarSesion) {
                cerrarSesion()
                true
            } else {
                false
            }
        }

        binding.cardTickets.setOnClickListener {
            startActivity(Intent(this, AdminTicketsActivity::class.java))
        }
        binding.cardTecnicos.setOnClickListener {
            startActivity(Intent(this, AdminTecnicosActivity::class.java))
        }
        binding.cardTiposReporte.setOnClickListener {
            startActivity(Intent(this, AdminReportTypesActivity::class.java))
        }

        // Reemplaza al boton "Actualizar dashboard".
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
            runCatching { repository.listarAdmin(null) }
                .onSuccess { reportes ->
                    val nuevos = reportes.count { it.status == "CREADO" }
                    val activos = reportes.count {
                        it.status == "APROBADO" || it.status == "ASIGNADO" ||
                            it.status == "EN_PROCESO" || it.status == "PAUSADO"
                    }
                    binding.textTicketsNuevos.text = nuevos.toString()
                    binding.textTicketsNuevosEtiqueta.text =
                        resources.getQuantityString(R.plurals.tickets_nuevos, nuevos)
                    binding.textTicketsResumen.text = if (activos == 0) {
                        getString(R.string.dashboard_sin_activos)
                    } else {
                        resources.getQuantityString(R.plurals.trabajos_pendientes, activos, activos)
                    }
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
