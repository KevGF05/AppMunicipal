package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminReportTypesBinding
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminReportTypesActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminReportTypesBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportTypeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportTypesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        adapter = ReportTypeAdapter()
        binding.recyclerTiposReporte.layoutManager = LinearLayoutManager(this)
        binding.recyclerTiposReporte.adapter = adapter

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonAgregarTipo.setOnClickListener { agregarTipo() }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.progressTipos.visibility = View.VISIBLE
        binding.textTiposVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.tiposReporteAdmin() }
                .onSuccess { tipos ->
                    val activos = tipos.filter { it.active }
                    adapter.submitList(activos)
                    binding.textCantidadTipos.text = "${activos.size} tipos disponibles para la ciudadanía"
                    binding.textTiposVacio.visibility = if (activos.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { mostrarError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun agregarTipo() {
        binding.layoutNuevoTipo.error = null
        val nombre = binding.editNuevoTipo.text?.toString()?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
        if (nombre.length < 3) {
            binding.layoutNuevoTipo.error = "Ingrese un nombre de al menos 3 caracteres."
            return
        }
        if (nombre.length > 80) {
            binding.layoutNuevoTipo.error = "Use un máximo de 80 caracteres."
            return
        }

        binding.progressTipos.visibility = View.VISIBLE
        binding.buttonAgregarTipo.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.crearTipoReporte(nombre) }
                .onSuccess {
                    binding.editNuevoTipo.text?.clear()
                    Snackbar.make(
                        binding.root,
                        "Tipo de reporte agregado. Ya está disponible para nuevos reportes anónimos.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    cargar()
                }
                .onFailure { mostrarError(it) }
            binding.progressTipos.visibility = View.GONE
            binding.buttonAgregarTipo.isEnabled = true
        }
    }

    private fun mostrarError(error: Throwable) {
        Snackbar.make(
            binding.root,
            error.message ?: "No fue posible completar la acción.",
            Snackbar.LENGTH_LONG
        ).show()
    }
}
