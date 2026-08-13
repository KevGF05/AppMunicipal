package com.example.appmunicipal.ui.publico

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.appmunicipal.R
import com.example.appmunicipal.databinding.ActivityConfirmacionReporteBinding
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets
import com.google.android.material.snackbar.Snackbar

/**
 * El codigo de seguimiento es la unica forma que tiene el ciudadano de volver a su
 * reporte. Antes solo se mostraba en pantalla: si lo perdia, el reporte quedaba
 * inaccesible. Ahora se puede copiar y compartir, y ademas queda guardado en
 * Mis reportes.
 */
class ConfirmacionReporteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmacionReporteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmacionReporteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)

        val code = intent.getStringExtra(EXTRA_CODIGO).orEmpty()
        val status = intent.getStringExtra(EXTRA_ESTADO).orEmpty()
        binding.textCodigo.text = code
        UiFormat.aplicarEstado(binding.textEstado, status)

        binding.buttonCopiar.setOnClickListener { copiarCodigo(code) }
        binding.buttonCompartir.setOnClickListener { compartirCodigo(code) }

        binding.buttonConsultar.setOnClickListener {
            startActivity(Intent(this, SeguimientoActivity::class.java).apply {
                putExtra(SeguimientoActivity.EXTRA_CODIGO, code)
            })
        }
        binding.buttonInicio.setOnClickListener { volverAlInicio() }

        // El gesto de retroceso lleva al inicio, no de vuelta al formulario ya enviado.
        onBackPressedDispatcher.addCallback(this) { volverAlInicio() }
    }

    private fun copiarCodigo(code: String) {
        if (code.isBlank()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.confirmacion_label_codigo), code)
        )
        Snackbar.make(binding.root, R.string.confirmacion_copiado, Snackbar.LENGTH_SHORT).show()
    }

    private fun compartirCodigo(code: String) {
        if (code.isBlank()) return
        val texto = getString(R.string.confirmacion_compartir_texto, code)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.confirmacion_compartir_titulo)))
    }

    private fun volverAlInicio() {
        startActivity(
            Intent(this, InicioActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_ESTADO = "estado"
    }
}
