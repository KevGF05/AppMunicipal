package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminReportTypesBinding
import com.example.appmunicipal.databinding.DialogReportTypeBinding
import com.example.appmunicipal.model.ReportTypeResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminReportTypesActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminReportTypesBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportTypeAdapter
    private var allTypes: List<ReportTypeResponse> = emptyList()
    private var showingInactive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportTypesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        adapter = ReportTypeAdapter(
            onEdit = { showTypeForm(it) },
            onToggle = { confirmStatusChange(it) }
        )
        binding.recyclerTiposReporte.layoutManager = LinearLayoutManager(this)
        binding.recyclerTiposReporte.adapter = adapter

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonAgregarTipo.setOnClickListener { showTypeForm(null) }
        binding.buttonVerDesactivados.setOnClickListener {
            showingInactive = !showingInactive
            renderCurrentList()
        }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) loadTypes()
    }

    private fun loadTypes() {
        binding.progressTipos.visibility = View.VISIBLE
        binding.textTiposVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.tiposReporteAdmin() }
                .onSuccess {
                    allTypes = it
                    renderCurrentList()
                }
                .onFailure { showError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun renderCurrentList() {
        val activeCount = allTypes.count { it.active }
        val inactiveCount = allTypes.size - activeCount
        val visible = allTypes.filter { if (showingInactive) !it.active else it.active }

        adapter.submitList(visible)
        binding.textSeccionTipos.text = if (showingInactive) "Reportes desactivados" else "Reportes disponibles"
        binding.textCantidadTipos.text = if (showingInactive) {
            "$inactiveCount tipos desactivados"
        } else {
            "$activeCount tipos activos"
        }
        binding.buttonVerDesactivados.text = if (showingInactive) {
            "← Ver reportes activos ($activeCount)"
        } else {
            "Ver reportes desactivados ($inactiveCount)"
        }
        binding.textTiposVacio.text = if (showingInactive) {
            "No hay tipos de reporte desactivados."
        } else {
            "Todavía no hay tipos de reporte activos."
        }
        binding.textTiposVacio.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showTypeForm(type: ReportTypeResponse?) {
        val dialogBinding = DialogReportTypeBinding.inflate(layoutInflater)
        val editing = type != null

        if (editing) {
            dialogBinding.editNombreTipo.setText(type?.name.orEmpty())
            dialogBinding.editNombreTipo.selectAll()
            dialogBinding.textAyudaTipo.text = "El cambio se aplicará a reportes futuros. Los tickets históricos conservan el nombre original."
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editing) "Editar tipo de reporte" else "Agregar tipo de reporte")
            .setView(dialogBinding.root)
            .setPositiveButton(if (editing) "Guardar" else "Agregar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.layoutNombreTipo.error = null
                val name = normalize(dialogBinding.editNombreTipo.text?.toString())
                if (!validateName(name) { dialogBinding.layoutNombreTipo.error = it }) {
                    return@setOnClickListener
                }
                dialog.dismiss()
                if (editing) updateType(type!!.id, name) else createType(name)
            }
        }
        dialog.show()
        dialogBinding.editNombreTipo.requestFocus()
    }

    private fun createType(name: String) {
        binding.progressTipos.visibility = View.VISIBLE
        binding.buttonAgregarTipo.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.crearTipoReporte(name) }
                .onSuccess {
                    showingInactive = false
                    showMessage("Tipo de reporte agregado y disponible para nuevos reportes.")
                    loadTypes()
                }
                .onFailure { showError(it) }
            binding.progressTipos.visibility = View.GONE
            binding.buttonAgregarTipo.isEnabled = true
        }
    }

    private fun updateType(id: Long, name: String) {
        binding.progressTipos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.editarTipoReporte(id, name) }
                .onSuccess {
                    showMessage("Tipo de reporte actualizado.")
                    loadTypes()
                }
                .onFailure { showError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun confirmStatusChange(type: ReportTypeResponse) {
        val activate = !type.active
        AlertDialog.Builder(this)
            .setTitle(if (activate) "Activar tipo" else "Desactivar tipo")
            .setMessage(
                if (activate) {
                    "${type.name} volverá a aparecer en el formulario de reportes."
                } else {
                    "${type.name} dejará de aparecer para nuevos reportes. Los tickets existentes no se eliminarán."
                }
            )
            .setPositiveButton(if (activate) "Activar" else "Desactivar") { _, _ ->
                changeStatus(type.id, activate)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changeStatus(id: Long, active: Boolean) {
        binding.progressTipos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.cambiarEstadoTipoReporte(id, active) }
                .onSuccess {
                    showMessage(if (active) "Tipo activado." else "Tipo desactivado.")
                    loadTypes()
                }
                .onFailure { showError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun normalize(value: String?): String = value.orEmpty().trim().replace(Regex("\\s+"), " ")

    private fun validateName(name: String, setError: (CharSequence?) -> Unit): Boolean {
        return when {
            name.length < 3 -> { setError("Ingrese un nombre de al menos 3 caracteres."); false }
            name.length > 80 -> { setError("Use un máximo de 80 caracteres."); false }
            else -> true
        }
    }

    private fun showError(error: Throwable) = showMessage(error.message ?: "No fue posible completar la acción.")
    private fun showMessage(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
}
