package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminDetalleBinding
import com.example.appmunicipal.databinding.DialogAsignarBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.model.TechnicianResponse
import com.example.appmunicipal.util.ImageLoader
import com.example.appmunicipal.util.UiFormat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminDetalleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDetalleBinding
    private val repository by lazy { AppRepository(this) }
    private val reportId by lazy { intent.getLongExtra(EXTRA_ID, -1L) }
    private var current: ReportResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonAprobar.setOnClickListener { aprobar() }
        binding.buttonRechazar.setOnClickListener { mostrarRechazo() }
        binding.buttonAsignar.setOnClickListener { mostrarAsignacion() }
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
        binding.textPrioridad.text = UiFormat.prioridad(report.priority)
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

        binding.buttonAprobar.visibility = if (report.status == "CREADO") View.VISIBLE else View.GONE
        binding.buttonRechazar.visibility = if (report.status == "CREADO" || report.status == "APROBADO") View.VISIBLE else View.GONE
        binding.buttonAsignar.visibility = if (report.status == "APROBADO" || report.status == "ASIGNADO") View.VISIBLE else View.GONE

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

    private fun mostrarAsignacion() {
        lifecycleScope.launch {
            runCatching { repository.tecnicos() }
                .onSuccess { technicians -> abrirDialogoAsignar(technicians) }
                .onFailure { mostrarError(it) }
        }
    }

    private fun abrirDialogoAsignar(technicians: List<TechnicianResponse>) {
        if (technicians.isEmpty()) {
            mostrarMensaje("No hay técnicos disponibles.")
            return
        }
        val dialogBinding = DialogAsignarBinding.inflate(layoutInflater)
        val labels = technicians.map { "${it.name} · ${it.email}" }
        dialogBinding.autoTecnico.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        dialogBinding.autoPrioridad.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("BAJA", "MEDIA", "ALTA", "URGENTE"))
        )
        current?.assignedTechnicianId?.let { id ->
            technicians.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index ->
                dialogBinding.autoTecnico.setText(labels[index], false)
            }
        }
        dialogBinding.autoPrioridad.setText(current?.priority ?: "MEDIA", false)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Asignar ticket")
            .setView(dialogBinding.root)
            .setPositiveButton("Asignar", null)
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
                ejecutar {
                    repository.asignar(
                        reportId,
                        technicians[index].id,
                        priority,
                        dialogBinding.editObservacion.text?.toString()?.trim()
                    )
                }
            }
        }
        dialog.show()
    }

    private fun ejecutar(action: suspend () -> ReportResponse) {
        binding.progressDetalle.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { action() }
                .onSuccess { mostrar(it); mostrarMensaje("Acción realizada correctamente.") }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
        }
    }

    private fun mostrarError(error: Throwable) = mostrarMensaje(error.message ?: "No fue posible completar la acción.")
    private fun mostrarMensaje(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    companion object { const val EXTRA_ID = "reporte_id" }
}
