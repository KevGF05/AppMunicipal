package com.example.appmunicipal.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminDashboardBinding
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }
    private lateinit var adapter: ReportAdapter
    private val filters = linkedMapOf(
        "Todos" to null,
        "Creados" to "CREADO",
        "Aprobados" to "APROBADO",
        "Asignados" to "ASIGNADO",
        "En proceso" to "EN_PROCESO",
        "Cerrados" to "CERRADO",
        "Rechazados" to "RECHAZADO"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textBienvenida.text = "Sesión: ${tokenStore.nombre()}"
        adapter = ReportAdapter { report ->
            startActivity(Intent(this, AdminDetalleActivity::class.java).putExtra(AdminDetalleActivity.EXTRA_ID, report.id))
        }
        binding.recyclerReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerReportes.adapter = adapter

        binding.autoFiltro.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, filters.keys.toList())
        )
        binding.autoFiltro.setText("Todos", false)
        binding.autoFiltro.setOnItemClickListener { _, _, _, _ -> cargar() }
        binding.buttonActualizar.setOnClickListener { cargar() }
        binding.buttonCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val filter = filters[binding.autoFiltro.text.toString()]
        binding.progressAdmin.visibility = View.VISIBLE
        binding.textVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarAdmin(filter) }
                .onSuccess { reports ->
                    adapter.submitList(reports)
                    binding.textVacio.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { error ->
                    Snackbar.make(binding.root, error.message ?: "No fue posible cargar los reportes.", Snackbar.LENGTH_LONG).show()
                }
            binding.progressAdmin.visibility = View.GONE
        }
    }

    private fun cerrarSesion() {
        tokenStore.limpiar()
        startActivity(Intent(this, InicioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
