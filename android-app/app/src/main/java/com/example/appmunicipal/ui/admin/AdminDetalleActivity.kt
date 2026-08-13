package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.R
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminDetalleBinding
import com.example.appmunicipal.databinding.DialogAsignarBinding
import com.example.appmunicipal.databinding.DialogMotivoBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.model.TechnicianResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.FechaFormat
import com.example.appmunicipal.util.ImageLoader
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
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
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar, titulo = getString(R.string.detalle_titulo))

        binding.buttonAprobar.setOnClickListener { confirmarAprobacion() }
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
        // El codigo pasa al subtitulo de la barra para que siga visible al desplazar.
        binding.toolbar.subtitle = report.code
        binding.textCodigo.text = report.code
        binding.textTipo.text = report.type

        // Estado y prioridad dejan de ser texto gris y pasan a ser etiquetas de color.
        UiFormat.aplicarEstado(binding.textEstado, report.status)
        UiFormat.aplicarPrioridad(binding.textPrioridad, report.priority)

        binding.textDireccion.text = report.address
        binding.textCoordenadas.text =
            getString(R.string.formato_coordenadas, report.latitude, report.longitude)
        binding.textDetalle.text = report.description
        binding.textTecnico.text =
            report.assignedTechnicianName ?: getString(R.string.detalle_sin_asignar)
        binding.textObservacion.text = when {
            report.status == "PAUSADO" && !report.pauseReason.isNullOrBlank() ->
                getString(R.string.detalle_motivo_pausa, report.pauseReason)
            !report.rejectionReason.isNullOrBlank() ->
                getString(R.string.detalle_motivo_rechazo, report.rejectionReason)
            !report.closureComment.isNullOrBlank() ->
                getString(R.string.detalle_cierre, report.closureComment)
            else -> getString(R.string.detalle_sin_observaciones)
        }

        binding.textHistorial.text = report.history.joinToString("\n\n") { item ->
            val fecha = FechaFormat.desdeIso(item.createdAt)
            val encabezado = buildString {
                append(UiFormat.estado(this@AdminDetalleActivity, item.newStatus))
                append(" · ")
                append(item.actor)
                if (fecha != null) {
                    append("\n")
                    append(fecha)
                }
            }
            val comentario = item.comment?.takeIf { it.isNotBlank() }
            if (comentario == null) encabezado else "$encabezado\n$comentario"
        }.ifBlank { getString(R.string.detalle_sin_historial) }

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
        val asignadoOEnProceso =
            report.status == "ASIGNADO" || report.status == "EN_PROCESO" || report.status == "PAUSADO"

        binding.buttonAprobar.visibility = if (creado) View.VISIBLE else View.GONE
        binding.buttonRechazar.visibility = if (creado || aprobado) View.VISIBLE else View.GONE
        binding.buttonAsignar.visibility =
            if (aprobado && report.assignedTechnicianId == null) View.VISIBLE else View.GONE
        binding.buttonReasignar.visibility =
            if (asignadoOEnProceso && report.assignedTechnicianId != null) View.VISIBLE else View.GONE

        val hayAcciones = creado || aprobado || asignadoOEnProceso
        binding.cardAcciones.visibility = if (hayAcciones) View.VISIBLE else View.GONE
    }

    /** Aprobar cambia el estado del ticket, asi que se confirma antes. */
    private fun confirmarAprobacion() {
        AlertDialog.Builder(this)
            .setTitle(R.string.admin_aprobar_titulo)
            .setMessage(R.string.admin_aprobar_mensaje)
            .setPositiveButton(R.string.admin_aprobar) { _, _ ->
                ejecutar { repository.aprobar(reportId) }
            }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    private fun mostrarRechazo() {
        val dialogBinding = DialogMotivoBinding.inflate(layoutInflater)
        dialogBinding.layoutMotivo.hint = getString(R.string.admin_rechazar_hint)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.admin_rechazar_titulo)
            .setMessage(R.string.admin_rechazar_mensaje)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.admin_rechazar, null)
            .setNegativeButton(R.string.accion_cancelar, null)
            .create()

        // El boton no cierra el dialogo hasta que el motivo sea valido: antes el
        // dialogo se cerraba y el error aparecia despues en un Snackbar.
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val motivo = dialogBinding.editMotivo.text?.toString()?.trim().orEmpty()
                if (motivo.length < 5) {
                    dialogBinding.layoutMotivo.error = getString(R.string.admin_rechazar_min)
                    return@setOnClickListener
                }
                dialogBinding.layoutMotivo.error = null
                dialog.dismiss()
                ejecutar { repository.rechazar(reportId, motivo) }
            }
        }
        dialog.show()
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
                getString(
                    if (reasignar) R.string.admin_sin_otro_tecnico else R.string.admin_sin_tecnicos
                )
            )
            return
        }

        val dialogBinding = DialogAsignarBinding.inflate(layoutInflater)
        val labels = disponibles.map { "${it.name} · ${it.email}" }
        val prioridades = listOf("BAJA", "MEDIA", "ALTA", "URGENTE")
        val etiquetasPrioridad = prioridades.map { UiFormat.prioridad(this, it) }

        dialogBinding.autoTecnico.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )
        dialogBinding.autoPrioridad.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, etiquetasPrioridad)
        )
        val prioridadActual = current?.priority ?: "MEDIA"
        dialogBinding.autoPrioridad.setText(UiFormat.prioridad(this, prioridadActual), false)
        dialogBinding.layoutObservacion.hint = getString(
            if (reasignar) R.string.admin_dialog_observacion_reasignar
            else R.string.admin_dialog_observacion
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (reasignar) R.string.admin_reasignar_titulo else R.string.admin_asignar_titulo)
            .setMessage(if (reasignar) R.string.admin_reasignar_mensaje else R.string.admin_asignar_mensaje)
            .setView(dialogBinding.root)
            .setPositiveButton(if (reasignar) R.string.accion_continuar else R.string.admin_asignar, null)
            .setNegativeButton(R.string.accion_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val index = labels.indexOf(dialogBinding.autoTecnico.text.toString())
                // La lista muestra la prioridad traducida, pero el backend espera el
                // valor original (BAJA, MEDIA, ALTA, URGENTE).
                val indicePrioridad = etiquetasPrioridad.indexOf(dialogBinding.autoPrioridad.text.toString())
                if (index < 0 || indicePrioridad < 0) {
                    mostrarMensaje(getString(R.string.admin_asignar_falta))
                    return@setOnClickListener
                }

                dialog.dismiss()
                val tecnico = disponibles[index]
                val priority = prioridades[indicePrioridad]
                val observacion = dialogBinding.editObservacion.text?.toString()?.trim()
                if (reasignar) {
                    ejecutar { repository.reasignar(reportId, tecnico.id, priority, observacion) }
                } else {
                    ejecutar { repository.asignar(reportId, tecnico.id, priority, observacion) }
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
                    mostrarMensaje(getString(R.string.detalle_accion_ok))
                }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
        }
    }

    private fun mostrarError(error: Throwable) =
        mostrarMensaje(error.message ?: getString(R.string.detalle_accion_error))

    private fun mostrarMensaje(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    companion object {
        const val EXTRA_ID = "reporte_id"
    }
}
