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
import com.example.appmunicipal.R
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityTecnicoDetalleBinding
import com.example.appmunicipal.databinding.DialogMotivoBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.ImageLoader
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
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
        } else {
            eliminarFotoTemporal()
        }
        actualizarBotones()
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) abrirCamara() else mostrarMensaje(getString(R.string.permiso_camara_cierre))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicoDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar, titulo = getString(R.string.detalle_titulo))

        // Al girar el telefono se perdia la fotografia de cierre ya tomada y habia que
        // repetirla. Aqui se recupera la ruta guardada antes de la rotacion.
        restaurarEstado(savedInstanceState)

        binding.buttonIniciar.setOnClickListener { iniciar() }
        binding.buttonPausar.setOnClickListener { solicitarMotivoPausa() }
        binding.buttonReanudar.setOnClickListener { reanudar() }
        binding.buttonFotoFinal.setOnClickListener { solicitarCamara() }
        binding.buttonCerrarTicket.setOnClickListener { cerrarTicket() }
        binding.editComentario.doAfterTextChanged { actualizarBotonesSinListener() }

        if (reportId <= 0) finish() else cargar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ESTADO_FOTO, finalPhotoFile?.absolutePath)
        outState.putBoolean(ESTADO_FOTO_LISTA, finalPhotoReady)
    }

    private fun restaurarEstado(savedInstanceState: Bundle?) {
        val ruta = savedInstanceState?.getString(ESTADO_FOTO) ?: return
        val archivo = File(ruta)
        if (!archivo.exists()) return

        finalPhotoFile = archivo
        finalPhotoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
        finalPhotoReady = savedInstanceState.getBoolean(ESTADO_FOTO_LISTA, false)
        if (finalPhotoReady) {
            binding.imageFinal.setImageURI(finalPhotoUri)
            binding.imageFinal.visibility = View.VISIBLE
        }
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
        binding.toolbar.subtitle = report.code
        binding.textCodigo.text = report.code
        binding.textTipo.text = report.type
        UiFormat.aplicarEstado(binding.textEstado, report.status)
        UiFormat.aplicarPrioridad(binding.textPrioridad, report.priority)
        binding.textDireccion.text = report.address
        binding.textDetalle.text = report.description

        val pausado = report.status == "PAUSADO" && !report.pauseReason.isNullOrBlank()
        binding.layoutPausaActual.visibility = if (pausado) View.VISIBLE else View.GONE
        binding.textMotivoPausa.text = report.pauseReason.orEmpty()

        binding.textCierreExistente.text = report.closureComment.orEmpty()
        binding.cardCierreExistente.visibility =
            if (report.closureComment.isNullOrBlank()) View.GONE else View.VISIBLE

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
        actualizarBotonesSinListener()
    }

    private fun actualizarBotonesSinListener() {
        val status = current?.status
        val canClose = status == "ASIGNADO" || status == "EN_PROCESO"
        val commentValid = (binding.editComentario.text?.toString()?.trim()?.length ?: 0) >= 10
        binding.buttonCerrarTicket.isEnabled = canClose && finalPhotoReady && commentValid
    }

    private fun iniciar() {
        ejecutarAccion(getString(R.string.tecnico_iniciado)) { repository.iniciar(reportId) }
    }

    private fun solicitarMotivoPausa() {
        // El campo del motivo ahora viene de dialog_motivo.xml. Antes se creaba a mano
        // con paddings en pixeles crudos y el error se mostraba dentro del EditText.
        val dialogBinding = DialogMotivoBinding.inflate(layoutInflater)
        dialogBinding.layoutMotivo.hint = getString(R.string.tecnico_pausa_hint)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.tecnico_pausa_titulo)
            .setMessage(R.string.tecnico_pausa_mensaje)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.tecnico_pausar, null)
            .setNegativeButton(R.string.accion_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val motivo = dialogBinding.editMotivo.text?.toString()?.trim().orEmpty()
                if (motivo.length !in 5..500) {
                    dialogBinding.layoutMotivo.error = getString(R.string.tecnico_pausa_min)
                    return@setOnClickListener
                }
                dialogBinding.layoutMotivo.error = null
                dialog.dismiss()
                ejecutarAccion(getString(R.string.tecnico_pausado)) { repository.pausar(reportId, motivo) }
            }
        }
        dialog.show()
    }

    private fun reanudar() {
        AlertDialog.Builder(this)
            .setTitle(R.string.tecnico_reanudar_titulo)
            .setMessage(R.string.tecnico_reanudar_mensaje)
            .setPositiveButton(R.string.tecnico_reanudar) { _, _ ->
                ejecutarAccion(getString(R.string.tecnico_reanudado)) { repository.reanudar(reportId) }
            }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    private fun ejecutarAccion(mensaje: String, action: suspend () -> ReportResponse) {
        binding.progressDetalle.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { action() }
                .onSuccess {
                    mostrar(it)
                    mostrarMensaje(mensaje)
                }
                .onFailure { mostrarError(it) }
            binding.progressDetalle.visibility = View.GONE
        }
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
        }.onFailure { mostrarMensaje(getString(R.string.registro_camara_error)) }
    }

    private fun cerrarTicket() {
        val comment = binding.editComentario.text?.toString()?.trim().orEmpty()
        val file = finalPhotoFile
        if (comment.length < 10 || file == null || !finalPhotoReady) {
            mostrarMensaje(getString(R.string.tecnico_cierre_incompleto))
            return
        }
        binding.progressDetalle.visibility = View.VISIBLE
        binding.buttonCerrarTicket.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.cerrar(reportId, comment, file) }
                .onSuccess {
                    current = it
                    eliminarFotoTemporal()
                    mostrar(it)
                    mostrarMensaje(getString(R.string.tecnico_cerrado))
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

    private fun mostrarError(error: Throwable) =
        mostrarMensaje(error.message ?: getString(R.string.detalle_accion_error))

    private fun mostrarMensaje(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    companion object {
        const val EXTRA_ID = "reporte_id"
        private const val ESTADO_FOTO = "foto_cierre_ruta"
        private const val ESTADO_FOTO_LISTA = "foto_cierre_lista"
    }
}
