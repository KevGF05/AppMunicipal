package com.example.appmunicipal.ui.tecnico

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityTecnicoTicketsBinding
import com.example.appmunicipal.notifications.NotificationScheduler
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TecnicoTicketsActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityTecnicoTicketsBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicoTicketsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        adapter = ReportAdapter { report ->
            startActivity(Intent(this, TecnicoDetalleActivity::class.java).putExtra(TecnicoDetalleActivity.EXTRA_ID, report.id))
        }
        binding.recyclerReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerReportes.adapter = adapter
        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonActualizar.setOnClickListener {
            cargar()
            NotificationScheduler.sincronizarAhora(this)
        }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.progressTecnico.visibility = View.VISIBLE
        binding.textVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarTecnico() }
                .onSuccess { values ->
                    val activos = values.filter { it.status in setOf("ASIGNADO", "EN_PROCESO", "PAUSADO") }
                    adapter.submitList(activos)
                    binding.textCantidad.text = if (activos.size == 1) "1 ticket activo" else "${activos.size} tickets activos"
                    binding.textVacio.visibility = if (activos.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { Snackbar.make(binding.root, it.message ?: "No fue posible cargar los tickets.", Snackbar.LENGTH_LONG).show() }
            binding.progressTecnico.visibility = View.GONE
        }
    }
}
