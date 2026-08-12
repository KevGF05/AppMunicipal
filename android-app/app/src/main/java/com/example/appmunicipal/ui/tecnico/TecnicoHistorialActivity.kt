package com.example.appmunicipal.ui.tecnico

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityTecnicoHistorialBinding
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TecnicoHistorialActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityTecnicoHistorialBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicoHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        adapter = ReportAdapter { report ->
            startActivity(Intent(this, TecnicoDetalleActivity::class.java).putExtra(TecnicoDetalleActivity.EXTRA_ID, report.id))
        }
        binding.recyclerHistorial.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistorial.adapter = adapter
        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonActualizar.setOnClickListener { cargar() }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.progressHistorial.visibility = View.VISIBLE
        binding.textVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarTecnico() }
                .onSuccess { values ->
                    val cerrados = values.filter { it.status == "CERRADO" }
                    adapter.submitList(cerrados)
                    binding.textCantidad.text = if (cerrados.size == 1) "1 trabajo completado" else "${cerrados.size} trabajos completados"
                    binding.textVacio.visibility = if (cerrados.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { Snackbar.make(binding.root, it.message ?: "No fue posible cargar el historial.", Snackbar.LENGTH_LONG).show() }
            binding.progressHistorial.visibility = View.GONE
        }
    }
}
