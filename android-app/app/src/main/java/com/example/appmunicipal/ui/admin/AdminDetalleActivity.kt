package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.databinding.ActivityAdminDetalleBinding
import com.example.appmunicipal.databinding.DialogAsignarBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.model.TechnicianResponse
import com.example.appmunicipal.util.ImageLoader
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AdminDetalleActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminDetalleBinding
    private val repository by lazy { AppRepository(this) }
    private val reportId by lazy { intent.getLongExtra(EXTRA_ID, -1L) }
    private var current: ReportResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonAprobar.setOnClickListener { aprobar() }
        binding.buttonRechazar.setOnClickListener { mostrarRechazo() }
        binding.buttonAsignar.setOnClickListener { mostrarAsignacion(reasignar = false) }
        binding.buttonReasignar.setOnClickListener { mostrarAsignacion(reasignar = true) }

        if (reportId <= 0) finish() else cargar()
    }

    private fun cargar() {
        binding.progressDetalle.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.obtenerAdmin(reportId) }
                .onSuccess { mostrar(it) }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
        }
    }

    private fun mostrar(report: ReportResponse) {
        current = report
        binding.textCodigo.text = report.code
        binding.textTipo.text = report.type
        binding.textEstado.text = UiFormat.estado(report.status)
        binding.textPrioridad.text = "Prioridad: ${UiFormat.prioridad(report.priority)}"
        binding.textDireccion.text = report.address
        binding.textCoordenadas.text = "${report.latitude}, ${report.longitude}"
        binding.textDetalle.text = report.description
        binding.textTecnico.text = report.assignedTechnicianName ?: "Sin asignar"
        binding.textObservacion.text = when {
            !report.rejectionReason.isNullOrBlank() -> "Motivo de rechazo: ${report.rejectionReason}"
            !report.closureComment.isNullOrBlank() -> "Cierre: ${report.closureComment}"
            else -> "Sin observaciones adicionales."
        }
        binding.textHistorial.text = report.history.joinToString("\n\n") { item ->
            "${UiFormat.estado(item.newStatus)} · ${item.actor}\n${item.comment.orEmpty()}"
        }.ifBlank { "Sin historial." }

        actualizarAcciones(report)

        lifecycleScope.launch { ImageLoader.cargar(report.initialPhotoUrl, binding.imageInicial) }
        if (!report.finalPhotoUrl.isNullOrBlank()) {
            binding.labelFotoFinal.visibility = View.VISIBLE
            binding.imageFinal.visibility = View.VISIBLE
            lifecycleScope.launch { ImageLoader.cargar(report.finalPhotoUrl, binding.imageFinal) }
        } else {
            binding.labelFotoFinal.visibility = View.GONE
            binding.imageFinal.visibility = View.GONE
        }
    }

    private fun actualizarAcciones(report: ReportResponse) {
        val creado = report.status == "CREADO"
        val aprobado = report.status == "APROBADO"
        val asignadoOEnProceso = report.status == "ASIGNADO" || report.status == "EN_PROCESO"

        binding.buttonAprobar.visibility = if (creado) View.VISIBLE else View.GONE
        binding.buttonRechazar.visibility = if (creado || aprobado) View.VISIBLE else View.GONE
        binding.buttonAsignar.visibility = if (aprobado && report.assignedTechnicianId == null) View.VISIBLE else View.GONE
        binding.buttonReasignar.visibility = if (asignadoOEnProceso && report.assignedTechnicianId != null) View.VISIBLE else View.GONE

        val hayAcciones = creado || aprobado || asignadoOEnProceso
        binding.cardAcciones.visibility = if (hayAcciones) View.VISIBLE else View.GONE
    }

    private fun aprobar() = ejecutar { repository.aprobar(reportId) }

    private fun mostrarRechazo() {
        val input = TextInputEditText(this).apply {
            hint = "Motivo del rechazo"
            minLines = 3
            setPadding(48, 16, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Rechazar reporte")
            .setMessage("Indique por qué el reporte no será atendido.")
            .setView(input)
            .setPositiveButton("Rechazar") { _, _ ->
                val reason = input.text?.toString()?.trim().orEmpty()
                if (reason.isBlank()) mostrarMensaje("El motivo es obligatorio.")
                else ejecutar { repository.rechazar(reportId, reason) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarAsignacion(reasignar: Boolean) {
        lifecycleScope.launch {
            runCatching { repository.tecnicosDisponibles() }
                .onSuccess { technicians -> abrirDialogoAsignar(technicians, reasignar) }
                .onFailure { mostrarError(it) }
        }
    }

    private fun abrirDialogoAsignar(
        technicians: List<TechnicianResponse>,
        reasignar: Boolean
    ) {
        val disponibles = if (reasignar) {
            technicians.filter { it.id != current?.assignedTechnicianId }
        } else {
            technicians
        }

        if (disponibles.isEmpty()) {
            mostrarMensaje(
                if (reasignar) "No hay otro técnico activo disponible para reasignar."
                else "No hay técnicos activos disponibles."
            )
            return
        }

        val dialogBinding = DialogAsignarBinding.inflate(layoutInflater)
        val labels = disponibles.map { "${it.name} · ${it.email}" }
        dialogBinding.autoTecnico.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )
        dialogBinding.autoPrioridad.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("BAJA", "MEDIA", "ALTA", "URGENTE"))
        )
        dialogBinding.autoPrioridad.setText(current?.priority ?: "MEDIA", false)
        dialogBinding.editObservacion.hint = if (reasignar) {
            "Motivo u observación de la reasignación"
        } else {
            "Observación para el técnico (opcional)"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (reasignar) "Reasignar técnico" else "Asignar técnico")
            .setMessage(
                if (reasignar) "Seleccione un técnico diferente. El ticket volverá al estado Asignado."
                else "Seleccione el técnico responsable y la prioridad."
            )
            .setView(dialogBinding.root)
            .setPositiveButton(if (reasignar) "Reasignar" else "Asignar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val index = labels.indexOf(dialogBinding.autoTecnico.text.toString())
                val priority = dialogBinding.autoPrioridad.text.toString()
                if (index < 0 || priority.isBlank()) {
                    mostrarMensaje("Seleccione técnico y prioridad.")
                    return@setOnClickListener
                }

                dialog.dismiss()
                val tecnico = disponibles[index]
                val observacion = dialogBinding.editObservacion.text?.toString()?.trim()
                if (reasignar) {
                    ejecutar {
                        repository.reasignar(reportId, tecnico.id, priority, observacion)
                    }
                } else {
                    ejecutar {
                        repository.asignar(reportId, tecnico.id, priority, observacion)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun ejecutar(action: suspend () -> ReportResponse) {
        binding.progressDetalle.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { action() }
                .onSuccess {
                    mostrar(it)
                    mostrarMensaje("Acción realizada correctamente.")
                }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
        }
    }

    private fun mostrarError(error: Throwable) =
        mostrarMensaje(error.message ?: "No fue posible completar la acción.")

    private fun mostrarMensaje(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    companion object {
        const val EXTRA_ID = "reporte_id"
    }
}
