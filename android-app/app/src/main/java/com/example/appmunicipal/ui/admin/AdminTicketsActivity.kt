package com.example.appmunicipal.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminTicketsBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminTicketsActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminTicketsBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportAdapter

    private val filtros = listOf(
        "Creados",
        "Asignados / en proceso",
        "Cerrados",
        "Rechazados"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminTicketsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        adapter = ReportAdapter { report ->
            startActivity(
                Intent(this, AdminDetalleActivity::class.java)
                    .putExtra(AdminDetalleActivity.EXTRA_ID, report.id)
            )
        }
        binding.recyclerReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerReportes.adapter = adapter

        binding.autoFiltro.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filtros))
        binding.autoFiltro.setText("Creados", false)
        binding.autoFiltro.setOnItemClickListener { _, _, _, _ -> cargar() }
        binding.buttonActualizar.setOnClickListener {
            cargar()
            NotificationScheduler.sincronizarAhora(this)
        }
        binding.buttonVolver.setOnClickListener { finish() }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.progressAdmin.visibility = View.VISIBLE
        binding.textVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarAdmin(null) }
                .onSuccess { reports ->
                    val filtrados = filtrar(reports)
                    adapter.submitList(filtrados)
                    binding.textVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: "No fue posible cargar los reportes.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.progressAdmin.visibility = View.GONE
        }
    }

    private fun filtrar(reports: List<ReportResponse>): List<ReportResponse> {
        return when (binding.autoFiltro.text.toString()) {
            "Asignados / en proceso" -> reports.filter { it.status == "ASIGNADO" || it.status == "EN_PROCESO" }
            "Cerrados" -> reports.filter { it.status == "CERRADO" }
            "Rechazados" -> reports.filter { it.status == "RECHAZADO" }
            else -> reports.filter { it.status == "CREADO" || it.status == "APROBADO" }
        }
    }
}
