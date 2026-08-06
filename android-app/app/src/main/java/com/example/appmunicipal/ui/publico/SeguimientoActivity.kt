package com.example.appmunicipal.ui.publico

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appmunicipal.data.local.SeguimientoDbHelper
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivitySeguimientoBinding
import com.example.appmunicipal.util.UiFormat
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

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonConsultar.setOnClickListener { consultar() }

        val requestedCode = intent.getStringExtra(EXTRA_CODIGO)
        val requestedToken = intent.getStringExtra(EXTRA_TOKEN)
        val saved = database.ultimo()
        binding.editCodigo.setText(requestedCode ?: saved?.codigo.orEmpty())
        binding.editToken.setText(requestedToken ?: saved?.token.orEmpty())
    }

    private fun consultar() {
        val code = binding.editCodigo.text?.toString()?.trim().orEmpty()
        val token = binding.editToken.text?.toString()?.trim().orEmpty()
        if (code.isBlank() || token.isBlank()) {
            Snackbar.make(binding.root, "Ingrese el código y el token de seguimiento.", Snackbar.LENGTH_LONG).show()
            return
        }
        binding.progressSeguimiento.visibility = View.VISIBLE
        binding.buttonConsultar.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.consultar(code, token) }
                .onSuccess { report ->
                    database.guardar(report.code, token, report.status)
                    binding.cardResultado.visibility = View.VISIBLE
                    binding.textResultadoCodigo.text = report.code
                    binding.textResultadoTipo.text = report.type
                    binding.textResultadoEstado.text = UiFormat.estado(report.status)
                    binding.textResultadoPrioridad.text = UiFormat.prioridad(report.priority)
                    binding.textResultadoDireccion.text = report.address
                    binding.textResultadoDetalle.text = report.description
                    binding.textResultadoObservacion.text = when {
                        !report.rejectionReason.isNullOrBlank() -> "Motivo: ${report.rejectionReason}"
                        !report.closureComment.isNullOrBlank() -> "Cierre: ${report.closureComment}"
                        else -> "El reporte continúa en gestión municipal."
                    }
                }
                .onFailure { error ->
                    Snackbar.make(binding.root, error.message ?: "No fue posible consultar.", Snackbar.LENGTH_LONG).show()
                }
            binding.progressSeguimiento.visibility = View.GONE
            binding.buttonConsultar.isEnabled = true
        }
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_TOKEN = "token"
    }
}
