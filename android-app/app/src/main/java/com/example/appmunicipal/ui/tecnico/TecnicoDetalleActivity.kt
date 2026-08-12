package com.example.appmunicipal.ui.tecnico

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityTecnicoDetalleBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.ImageLoader
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File

class TecnicoDetalleActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityTecnicoDetalleBinding
    private val repository by lazy { AppRepository(this) }
    private val reportId by lazy { intent.getLongExtra(EXTRA_ID, -1L) }
    private var current: ReportResponse? = null
    private var finalPhotoFile: File? = null
    private var finalPhotoUri: Uri? = null
    private var finalPhotoReady = false

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        finalPhotoReady = saved && finalPhotoFile?.exists() == true
        if (finalPhotoReady) {
            binding.imageFinal.setImageURI(finalPhotoUri)
            binding.imageFinal.visibility = View.VISIBLE
        } else eliminarFotoTemporal()
        actualizarBotones()
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) abrirCamara() else mostrarMensaje("Se necesita permiso de cámara para cerrar el ticket.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicoDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonIniciar.setOnClickListener { iniciar() }
        binding.buttonPausar.setOnClickListener { solicitarMotivoPausa() }
        binding.buttonReanudar.setOnClickListener { reanudar() }
        binding.buttonFotoFinal.setOnClickListener { solicitarCamara() }
        binding.buttonCerrarTicket.setOnClickListener { cerrarTicket() }
        binding.editComentario.doAfterTextChanged { actualizarBotonesSinListener() }

        if (reportId <= 0) finish() else cargar()
    }

    private fun cargar() {
        binding.progressDetalle.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.obtenerTecnico(reportId) }
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
        binding.textDetalle.text = report.description

        val pausado = report.status == "PAUSADO" && !report.pauseReason.isNullOrBlank()
        binding.layoutPausaActual.visibility = if (pausado) View.VISIBLE else View.GONE
        binding.textMotivoPausa.text = report.pauseReason.orEmpty()

        binding.textCierreExistente.text = report.closureComment.orEmpty()
        binding.textCierreExistente.visibility = if (report.closureComment.isNullOrBlank()) View.GONE else View.VISIBLE

        lifecycleScope.launch { ImageLoader.cargar(report.initialPhotoUrl, binding.imageInicial) }
        if (!report.finalPhotoUrl.isNullOrBlank()) {
            binding.imageFinal.visibility = View.VISIBLE
            lifecycleScope.launch { ImageLoader.cargar(report.finalPhotoUrl, binding.imageFinal) }
        } else if (report.status != "CERRADO" && !finalPhotoReady) {
            binding.imageFinal.visibility = View.GONE
        }
        actualizarBotones()
    }

    private fun actualizarBotones() {
        val status = current?.status
        binding.buttonIniciar.visibility = if (status == "ASIGNADO") View.VISIBLE else View.GONE
        binding.buttonPausar.visibility = if (status == "EN_PROCESO") View.VISIBLE else View.GONE
        binding.buttonReanudar.visibility = if (status == "PAUSADO") View.VISIBLE else View.GONE

        val canClose = status == "ASIGNADO" || status == "EN_PROCESO"
        val closed = status == "CERRADO"
        binding.layoutCierre.visibility = if (canClose || closed) View.VISIBLE else View.GONE
        binding.buttonFotoFinal.visibility = if (canClose) View.VISIBLE else View.GONE
        binding.layoutComentarioCierre.visibility = if (canClose) View.VISIBLE else View.GONE
        binding.buttonCerrarTicket.visibility = if (canClose) View.VISIBLE else View.GONE
        val commentValid = (binding.editComentario.text?.toString()?.trim()?.length ?: 0) >= 10
        binding.buttonCerrarTicket.isEnabled = canClose && finalPhotoReady && commentValid
    }

    private fun actualizarBotonesSinListener() {
        val status = current?.status
        val canClose = status == "ASIGNADO" || status == "EN_PROCESO"
        val commentValid = (binding.editComentario.text?.toString()?.trim()?.length ?: 0) >= 10
        binding.buttonCerrarTicket.isEnabled = canClose && finalPhotoReady && commentValid
    }

    private fun iniciar() {
        ejecutarAccion("Trabajo iniciado.") { repository.iniciar(reportId) }
    }

    private fun solicitarMotivoPausa() {
        val input = TextInputEditText(this).apply {
            hint = "Ej. Falta repuesto o se requiere apoyo adicional"
            minLines = 3
            maxLines = 5
            setPadding(44, 16, 44, 16)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Pausar trabajo")
            .setMessage("Indique el motivo. Quedará registrado en el historial del ticket.")
            .setView(input)
            .setPositiveButton("Pausar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val motivo = input.text?.toString()?.trim().orEmpty()
                if (motivo.length !in 5..500) {
                    input.error = "Ingrese un motivo de al menos 5 caracteres."
                    return@setOnClickListener
                }
                dialog.dismiss()
                ejecutarAccion("Ticket pausado.") { repository.pausar(reportId, motivo) }
            }
        }
        dialog.show()
    }

    private fun reanudar() {
        AlertDialog.Builder(this)
            .setTitle("Reanudar trabajo")
            .setMessage("El ticket volverá al estado En proceso.")
            .setPositiveButton("Reanudar") { _, _ ->
                ejecutarAccion("Trabajo reanudado.") { repository.reanudar(reportId) }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarAccion(mensaje: String, action: suspend () -> ReportResponse) {
        binding.progressDetalle.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { action() }
                .onSuccess { mostrar(it); mostrarMensaje(mensaje) }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
        }
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun abrirCamara() {
        runCatching {
            eliminarFotoTemporal()
            val directory = File(filesDir, "cierres").apply { mkdirs() }
            val file = File.createTempFile("CIERRE_${System.currentTimeMillis()}_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            finalPhotoFile = file
            finalPhotoUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { mostrarMensaje("No fue posible preparar la cámara.") }
    }

    private fun cerrarTicket() {
        val comment = binding.editComentario.text?.toString()?.trim().orEmpty()
        val file = finalPhotoFile
        if (comment.length < 10 || file == null || !finalPhotoReady) {
            mostrarMensaje("Agregue una foto final y un comentario de al menos 10 caracteres.")
            return
        }
        binding.progressDetalle.visibility = View.VISIBLE
        binding.buttonCerrarTicket.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.cerrar(reportId, comment, file) }
                .onSuccess {
                    current = it
                    mostrar(it)
                    mostrarMensaje("Ticket cerrado correctamente.")
                    eliminarFotoTemporal()
                }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
            actualizarBotonesSinListener()
        }
    }

    private fun eliminarFotoTemporal() {
        finalPhotoFile?.takeIf { it.exists() }?.delete()
        finalPhotoFile = null
        finalPhotoUri = null
        finalPhotoReady = false
    }

    private fun mostrarError(error: Throwable) = mostrarMensaje(error.message ?: "No fue posible completar la acción.")
    private fun mostrarMensaje(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    companion object { const val EXTRA_ID = "reporte_id" }
}
