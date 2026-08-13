package com.example.appmunicipal.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.R
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminTicketsBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminTicketsActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminTicketsBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminTicketsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        adapter = ReportAdapter { report ->
            startActivity(
                Intent(this, AdminDetalleActivity::class.java)
                    .putExtra(AdminDetalleActivity.EXTRA_ID, report.id)
            )
        }
        binding.recyclerReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerReportes.adapter = adapter

        // El filtro pasa de una lista desplegable a chips visibles: se ve el filtro
        // activo sin abrir nada y se cambia con un toque.
        // Antes se comparaba el texto en espanol del desplegable; ahora se compara el
        // id del chip, que no depende del idioma ni de la ortografia.
        binding.chipGroupFiltro.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                // ChipGroup con selectionRequired ya lo evita, pero si el usuario
                // deselecciona se vuelve al filtro por defecto en vez de quedar vacio.
                group.check(R.id.chipPendientes)
            } else {
                cargar()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            cargar()
            NotificationScheduler.sincronizarAhora(this)
        }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.swipeRefresh.isRefreshing = true
        binding.layoutVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarAdmin(null) }
                .onSuccess { reports ->
                    val filtrados = filtrar(reports)
                    adapter.submitList(filtrados)
                    binding.layoutVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        error.message ?: getString(R.string.tickets_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun filtrar(reports: List<ReportResponse>): List<ReportResponse> {
        return when (binding.chipGroupFiltro.checkedChipId) {
            R.id.chipEnCurso -> reports.filter {
                it.status == "ASIGNADO" || it.status == "EN_PROCESO" || it.status == "PAUSADO"
            }
            R.id.chipCerrados -> reports.filter { it.status == "CERRADO" }
            R.id.chipRechazados -> reports.filter { it.status == "RECHAZADO" }
            else -> reports.filter { it.status == "CREADO" || it.status == "APROBADO" }
        }
    }
}
