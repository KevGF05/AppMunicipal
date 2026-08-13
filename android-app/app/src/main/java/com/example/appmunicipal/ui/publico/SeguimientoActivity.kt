package com.example.appmunicipal.ui.publico

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.SeguimientoDbHelper
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivitySeguimientoBinding
import com.example.appmunicipal.util.FechaFormat
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SeguimientoActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySeguimientoBinding
    private val repository by lazy { AppRepository(this) }
    private val database by lazy { SeguimientoDbHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeguimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        binding.buttonConsultar.setOnClickListener { consultar() }

        val requestedCode = intent.getStringExtra(EXTRA_CODIGO)
        val saved = database.ultimo()
        binding.editCodigo.setText(requestedCode ?: saved?.codigo.orEmpty())
        if (!requestedCode.isNullOrBlank()) consultar()
    }

    private fun consultar() {
        val code = binding.editCodigo.text?.toString()?.trim()?.uppercase().orEmpty()
        if (code.isBlank()) {
            Snackbar.make(binding.root, R.string.seguimiento_ingrese_codigo, Snackbar.LENGTH_LONG).show()
            return
        }
        binding.progressSeguimiento.visibility = View.VISIBLE
        binding.buttonConsultar.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.consultar(code) }
                .onSuccess { report ->
                    database.guardar(report.code, report.status)
                    binding.cardResultado.visibility = View.VISIBLE
                    binding.textResultadoCodigo.text = report.code
                    UiFormat.aplicarEstado(binding.textResultadoEstado, report.status)
                    binding.textResultadoMensaje.text = report.message

                    val mostrarPausa = report.status == "PAUSADO" && !report.pauseReason.isNullOrBlank()
                    binding.cardMotivoPausa.visibility = if (mostrarPausa) View.VISIBLE else View.GONE
                    binding.textMotivoPausa.text = report.pauseReason.orEmpty()

                    val mostrarCierre = report.status == "CERRADO" && !report.closureComment.isNullOrBlank()
                    binding.cardTrabajoRealizado.visibility = if (mostrarCierre) View.VISIBLE else View.GONE
                    binding.textTrabajoRealizado.text = report.closureComment.orEmpty()

                    val actualizado = FechaFormat.desdeIso(report.updatedAt)
                    binding.textResultadoActualizado.text = if (actualizado == null) {
                        getString(R.string.seguimiento_sin_actualizacion)
                    } else {
                        getString(R.string.seguimiento_actualizado, actualizado)
                    }
                }
                .onFailure { error ->
                    binding.cardResultado.visibility = View.GONE
                    binding.cardMotivoPausa.visibility = View.GONE
                    binding.cardTrabajoRealizado.visibility = View.GONE
                    Snackbar.make(
                        binding.root,
                        error.message ?: getString(R.string.seguimiento_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.progressSeguimiento.visibility = View.GONE
            binding.buttonConsultar.isEnabled = true
        }
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
    }
}
