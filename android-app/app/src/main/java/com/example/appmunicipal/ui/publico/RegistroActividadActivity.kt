package com.example.appmunicipal.ui.publico

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.SeguimientoDbHelper
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityRegistroActividadBinding
import com.example.appmunicipal.validation.RegistroActividadInput
import com.example.appmunicipal.validation.RegistroActividadValidator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import com.example.appmunicipal.util.applySafeSystemInsets

class RegistroActividadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroActividadBinding
    private val repository by lazy { AppRepository(this) }
    private val database by lazy { SeguimientoDbHelper(this) }

    private var fotoUri: Uri? = null
    private var fotoArchivo: File? = null
    private var fotoConfirmada = false
    private var enviando = false
    private var ubicacionLatitud: Double? = null
    private var ubicacionLongitud: Double? = null
    private val modoTecnico by lazy { intent.getBooleanExtra(EXTRA_MODO_TECNICO, false) }

    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        if (guardada && fotoArchivo?.exists() == true) {
            fotoConfirmada = true
            eliminarMetadatosSensibles(fotoArchivo)
            binding.imageEvidencia.setPadding(0, 0, 0, 0)
            binding.imageEvidencia.setImageURI(fotoUri)
            binding.textErrorFoto.visibility = View.GONE
        } else {
            fotoConfirmada = false
            eliminarFotoTemporal()
            restablecerVistaFoto()
            Snackbar.make(binding.root, "No se guardó la fotografía.", Snackbar.LENGTH_LONG).show()
        }
        actualizarEstadoBotonRegistrar()
    }

    private val permisoCamaraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) abrirCamara() else mostrarPermisoDenegado()
        }

    private val seleccionarUbicacionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val datos = resultado.data ?: return@registerForActivityResult
            val direccion = datos.getStringExtra(MapaUbicacionActivity.EXTRA_DIRECCION).orEmpty()
            val latitud = datos.getDoubleExtra(MapaUbicacionActivity.EXTRA_LATITUD, Double.NaN)
            val longitud = datos.getDoubleExtra(MapaUbicacionActivity.EXTRA_LONGITUD, Double.NaN)
            if (direccion.isNotBlank() && latitud.isFinite() && longitud.isFinite()) {
                ubicacionLatitud = latitud
                ubicacionLongitud = longitud
                binding.editUbicacion.setText(direccion)
                binding.textCoordenadas.text = getString(R.string.formato_coordenadas, latitud, longitud)
                binding.layoutUbicacion.error = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroActividadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()
        configurarLista()
        configurarAcciones()
        if (modoTecnico) {
            binding.textTituloRegistro.text = "Crear ticket de patrullaje"
            binding.textSubtituloRegistro.text = "Registre una incidencia detectada durante su recorrido. El ticket será enviado al panel administrativo para su revisión."
            binding.buttonRegistrar.text = "Crear ticket"
        }
        actualizarEstadoBotonRegistrar()
    }

    private fun configurarLista() {
        val respaldo = listOf(
            "Señal de tránsito caída",
            "Señal de tránsito dañada",
            "Semáforo dañado",
            "Automóvil mal estacionado"
        )
        aplicarTiposReporte(respaldo)

        lifecycleScope.launch {
            runCatching { repository.tiposReportePublicos() }
                .onSuccess { tipos ->
                    val nombres = tipos.filter { it.active }.map { it.name }
                    if (nombres.isNotEmpty()) aplicarTiposReporte(nombres)
                }
                .onFailure {
                    Snackbar.make(
                        binding.root,
                        "No fue posible actualizar los tipos de reporte. Se muestran las opciones predeterminadas.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun aplicarTiposReporte(tipos: List<String>) {
        val actual = binding.autoTipoTrabajo.text?.toString().orEmpty()
        binding.autoTipoTrabajo.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, tipos)
        )
        if (actual.isNotBlank() && actual !in tipos) {
            binding.autoTipoTrabajo.setText("", false)
        }
    }

    private fun configurarAcciones() {
        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonSeleccionarMapa.setOnClickListener { abrirSelectorUbicacion() }
        binding.buttonTomarFoto.setOnClickListener { solicitarCamara() }
        binding.buttonRegistrar.setOnClickListener { registrarActividad() }
        binding.autoTipoTrabajo.setOnItemClickListener { _, _, _, _ -> binding.layoutTipoTrabajo.error = null }
        binding.checkConsentimiento.setOnCheckedChangeListener { _, marcado ->
            if (marcado) binding.textErrorConsentimiento.visibility = View.GONE
        }
    }

    private fun abrirSelectorUbicacion() {
        seleccionarUbicacionLauncher.launch(Intent(this, MapaUbicacionActivity::class.java).apply {
            ubicacionLatitud?.let { putExtra(MapaUbicacionActivity.EXTRA_LATITUD, it) }
            ubicacionLongitud?.let { putExtra(MapaUbicacionActivity.EXTRA_LONGITUD, it) }
            putExtra(MapaUbicacionActivity.EXTRA_DIRECCION, binding.editUbicacion.text?.toString().orEmpty())
        })
    }

    private fun solicitarCamara() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> abrirCamara()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> AlertDialog.Builder(this)
                .setTitle("Permiso de cámara")
                .setMessage("La fotografía es obligatoria para enviar el reporte anónimo.")
                .setPositiveButton("Continuar") { _, _ -> permisoCamaraLauncher.launch(Manifest.permission.CAMERA) }
                .setNegativeButton("Cancelar", null)
                .show()
            else -> permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Snackbar.make(binding.root, "Este dispositivo no tiene una cámara disponible.", Snackbar.LENGTH_LONG).show()
            return
        }
        runCatching {
            eliminarFotoTemporal()
            val directory = File(filesDir, "evidencias").apply { mkdirs() }
            val file = File.createTempFile("EVIDENCIA_${System.currentTimeMillis()}_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            fotoArchivo = file
            fotoUri = uri
            fotoConfirmada = false
            actualizarEstadoBotonRegistrar()
            tomarFotoLauncher.launch(uri)
        }.onFailure {
            Snackbar.make(binding.root, "No fue posible preparar la cámara.", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun mostrarPermisoDenegado() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setTitle("Cámara deshabilitada")
                .setMessage("Active el permiso de cámara en la configuración. Sin foto no se puede enviar.")
                .setPositiveButton("Abrir configuración") { _, _ ->
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else Snackbar.make(binding.root, "Sin cámara no se puede enviar el reporte.", Snackbar.LENGTH_LONG).show()
    }

    private fun registrarActividad() {
        limpiarErrores()
        val input = RegistroActividadInput(
            tipoTrabajo = binding.autoTipoTrabajo.text.toString().trim(),
            ubicacion = binding.editUbicacion.text?.toString()?.trim().orEmpty(),
            latitud = ubicacionLatitud,
            longitud = ubicacionLongitud,
            detalle = binding.editDetalle.text?.toString()?.trim().orEmpty(),
            tieneFoto = fotoConfirmada && fotoArchivo?.exists() == true,
            aceptaProteccionDatos = binding.checkConsentimiento.isChecked
        )
        val errores = RegistroActividadValidator.validar(input)
        mostrarErrores(errores)
        if (!errores.esValido) {
            Snackbar.make(binding.root, "Revise los campos marcados.", Snackbar.LENGTH_LONG).show()
            return
        }
        val file = fotoArchivo ?: return
        enviando = true
        mostrarCarga(true)
        lifecycleScope.launch {
            runCatching {
                repository.crearReporte(
                    input.tipoTrabajo, input.detalle, input.ubicacion,
                    requireNotNull(input.latitud), requireNotNull(input.longitud), file
                )
            }.onSuccess { response ->
                database.guardar(response.code, response.status)
                if (modoTecnico) {
                    eliminarFotoTemporal()
                    AlertDialog.Builder(this@RegistroActividadActivity)
                        .setTitle("Ticket creado")
                        .setMessage("El reporte ${response.code} fue enviado al panel administrativo para revisión y asignación.")
                        .setPositiveButton("Volver al dashboard") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    startActivity(Intent(this@RegistroActividadActivity, ConfirmacionReporteActivity::class.java).apply {
                        putExtra(ConfirmacionReporteActivity.EXTRA_CODIGO, response.code)
                        putExtra(ConfirmacionReporteActivity.EXTRA_ESTADO, response.status)
                    })
                    finish()
                }
            }.onFailure { error ->
                Snackbar.make(binding.root, error.message ?: "No fue posible enviar el reporte.", Snackbar.LENGTH_LONG).show()
            }
            enviando = false
            mostrarCarga(false)
        }
    }

    private fun mostrarCarga(visible: Boolean) {
        binding.progressEnvio.visibility = if (visible) View.VISIBLE else View.GONE
        binding.buttonSeleccionarMapa.isEnabled = !visible
        binding.buttonTomarFoto.isEnabled = !visible
        actualizarEstadoBotonRegistrar()
    }

    private fun mostrarErrores(errores: com.example.appmunicipal.validation.RegistroActividadErrors) {
        binding.layoutTipoTrabajo.error = errores.tipoTrabajo
        binding.layoutUbicacion.error = errores.ubicacion
        binding.layoutDetalle.error = errores.detalle
        binding.textErrorFoto.text = errores.foto.orEmpty()
        binding.textErrorFoto.visibility = if (errores.foto == null) View.GONE else View.VISIBLE
        binding.textErrorConsentimiento.text = errores.consentimiento.orEmpty()
        binding.textErrorConsentimiento.visibility = if (errores.consentimiento == null) View.GONE else View.VISIBLE
    }

    private fun limpiarErrores() {
        binding.layoutTipoTrabajo.error = null
        binding.layoutUbicacion.error = null
        binding.layoutDetalle.error = null
        binding.textErrorFoto.visibility = View.GONE
        binding.textErrorConsentimiento.visibility = View.GONE
    }

    private fun restablecerVistaFoto() {
        binding.imageEvidencia.setImageResource(R.drawable.ic_camera_alt_24)
        val padding = resources.getDimensionPixelSize(R.dimen.evidence_placeholder_padding)
        binding.imageEvidencia.setPadding(padding, padding, padding, padding)
    }

    private fun actualizarEstadoBotonRegistrar() {
        binding.buttonRegistrar.isEnabled = !enviando && fotoConfirmada && fotoArchivo?.exists() == true
    }

    private fun eliminarMetadatosSensibles(archivo: File?) {
        if (archivo == null || !archivo.exists()) return
        runCatching {
            val exif = ExifInterface(archivo.absolutePath)
            listOf(
                ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP
            ).forEach { exif.setAttribute(it, null) }
            exif.saveAttributes()
        }
    }

    private fun eliminarFotoTemporal() {
        fotoArchivo?.takeIf { it.exists() }?.delete()
        fotoArchivo = null
        fotoUri = null
        fotoConfirmada = false
    }

    companion object {
        const val EXTRA_MODO_TECNICO = "modo_tecnico"
    }
}
