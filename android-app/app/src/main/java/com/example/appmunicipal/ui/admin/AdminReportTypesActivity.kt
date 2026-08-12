package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminReportTypesBinding
import com.example.appmunicipal.model.ReportTypeResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
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

        adapter = ReportTypeAdapter(
            onEdit = { mostrarEdicion(it) },
            onToggle = { confirmarCambioEstado(it) }
        )
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
                    adapter.submitList(tipos)
                    val activos = tipos.count { it.active }
                    val inactivos = tipos.size - activos
                    binding.textCantidadTipos.text = "$activos activos · $inactivos inactivos"
                    binding.textTiposVacio.visibility = if (tipos.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { mostrarError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun agregarTipo() {
        binding.layoutNuevoTipo.error = null
        val nombre = normalizar(binding.editNuevoTipo.text?.toString())
        if (!validarNombre(nombre, binding.layoutNuevoTipo::setError)) return

        binding.progressTipos.visibility = View.VISIBLE
        binding.buttonAgregarTipo.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.crearTipoReporte(nombre) }
                .onSuccess {
                    binding.editNuevoTipo.text?.clear()
                    mostrarMensaje("Tipo de reporte agregado y disponible para nuevos reportes.")
                    cargar()
                }
                .onFailure { mostrarError(it) }
            binding.progressTipos.visibility = View.GONE
            binding.buttonAgregarTipo.isEnabled = true
        }
    }

    private fun mostrarEdicion(tipo: ReportTypeResponse) {
        val input = TextInputEditText(this).apply {
            setText(tipo.name)
            selectAll()
            maxLines = 2
            setPadding(44, 20, 44, 20)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar tipo de reporte")
            .setMessage("El nuevo nombre se usará en reportes futuros. Los tickets históricos conservarán el nombre con el que fueron creados.")
            .setView(input)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = normalizar(input.text?.toString())
                if (nombre.length !in 3..80) {
                    input.error = "Ingrese entre 3 y 80 caracteres."
                    return@setOnClickListener
                }
                dialog.dismiss()
                editar(tipo.id, nombre)
            }
        }
        dialog.show()
    }

    private fun editar(id: Long, nombre: String) {
        binding.progressTipos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.editarTipoReporte(id, nombre) }
                .onSuccess {
                    mostrarMensaje("Tipo de reporte actualizado.")
                    cargar()
                }
                .onFailure { mostrarError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun confirmarCambioEstado(tipo: ReportTypeResponse) {
        val activar = !tipo.active
        AlertDialog.Builder(this)
            .setTitle(if (activar) "Activar tipo" else "Desactivar tipo")
            .setMessage(
                if (activar) {
                    "${tipo.name} volverá a aparecer en el formulario de reportes."
                } else {
                    "${tipo.name} dejará de aparecer para nuevos reportes. Los tickets existentes no se eliminarán."
                }
            )
            .setPositiveButton(if (activar) "Activar" else "Desactivar") { _, _ ->
                cambiarEstado(tipo.id, activar)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarEstado(id: Long, activo: Boolean) {
        binding.progressTipos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.cambiarEstadoTipoReporte(id, activo) }
                .onSuccess {
                    mostrarMensaje(if (activo) "Tipo activado." else "Tipo desactivado.")
                    cargar()
                }
                .onFailure { mostrarError(it) }
            binding.progressTipos.visibility = View.GONE
        }
    }

    private fun normalizar(value: String?): String = value.orEmpty().trim().replace(Regex("\\s+"), " ")

    private fun validarNombre(nombre: String, setError: (CharSequence?) -> Unit): Boolean {
        return when {
            nombre.length < 3 -> { setError("Ingrese un nombre de al menos 3 caracteres."); false }
            nombre.length > 80 -> { setError("Use un máximo de 80 caracteres."); false }
            else -> true
        }
    }

    private fun mostrarError(error: Throwable) = mostrarMensaje(error.message ?: "No fue posible completar la acción.")
    private fun mostrarMensaje(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
}
