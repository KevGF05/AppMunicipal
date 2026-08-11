package com.example.appmunicipal.ui.tecnico

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.databinding.ActivityTecnicoDashboardBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.auth.CambiarPasswordActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TecnicoDashboardActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityTecnicoDashboardBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }
    private lateinit var adapter: ReportAdapter
    private var reportes: List<ReportResponse> = emptyList()

    private val filtros = listOf("Asignados", "Cerrados")

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

        adapter = ReportAdapter { report ->
            startActivity(
                Intent(this, TecnicoDetalleActivity::class.java)
                    .putExtra(TecnicoDetalleActivity.EXTRA_ID, report.id)
            )
        }
        binding.recyclerReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerReportes.adapter = adapter

        binding.autoFiltroTecnico.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, filtros)
        )
        binding.autoFiltroTecnico.setText("Asignados", false)
        binding.autoFiltroTecnico.setOnItemClickListener { _, _, _, _ -> aplicarFiltro() }

        binding.buttonActualizar.setOnClickListener {
            cargar()
            NotificationScheduler.sincronizarAhora(this)
        }
        binding.buttonCerrarSesion.setOnClickListener { cerrarSesion() }

        solicitarPermisoNotificaciones()
        NotificationScheduler.programar(this)
    }

    override fun onPersonalSessionResume() {
        cargar()
        NotificationScheduler.sincronizarAhora(this)
    }

    private fun cargar() {
        binding.progressTecnico.visibility = View.VISIBLE
        binding.textVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarTecnico() }
                .onSuccess {
                    reportes = it
                    aplicarFiltro()
                }
                .onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: "No fue posible cargar los tickets.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.progressTecnico.visibility = View.GONE
        }
    }

    private fun aplicarFiltro() {
        val cerrados = binding.autoFiltroTecnico.text.toString() == "Cerrados"
        val filtrados = if (cerrados) {
            reportes.filter { it.status == "CERRADO" }
        } else {
            reportes.filter { it.status == "ASIGNADO" || it.status == "EN_PROCESO" }
        }

        adapter.submitList(filtrados)
        binding.textVacio.text = if (cerrados) {
            "No tiene tickets cerrados."
        } else {
            "No tiene tickets asignados o en proceso."
        }
        binding.textVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
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
