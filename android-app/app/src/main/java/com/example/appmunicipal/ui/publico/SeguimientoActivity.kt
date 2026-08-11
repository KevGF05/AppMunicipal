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
import com.example.appmunicipal.util.applySafeSystemInsets

class SeguimientoActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySeguimientoBinding
    private val repository by lazy { AppRepository(this) }
    private val database by lazy { SeguimientoDbHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeguimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonConsultar.setOnClickListener { consultar() }

        val requestedCode = intent.getStringExtra(EXTRA_CODIGO)
        val saved = database.ultimo()
        binding.editCodigo.setText(requestedCode ?: saved?.codigo.orEmpty())
    }

    private fun consultar() {
        val code = binding.editCodigo.text?.toString()?.trim()?.uppercase().orEmpty()
        if (code.isBlank()) {
            Snackbar.make(binding.root, "Ingrese el código de seguimiento.", Snackbar.LENGTH_LONG).show()
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
                    binding.textResultadoEstado.text = UiFormat.estado(report.status)
                    binding.textResultadoMensaje.text = report.message
                    binding.textResultadoActualizado.text = report.updatedAt
                        ?.replace("T", " ")
                        ?.substringBefore('.')
                        ?.let { "Última actualización: $it" }
                        ?: "Última actualización no disponible"
                }
                .onFailure { error ->
                    binding.cardResultado.visibility = View.GONE
                    Snackbar.make(binding.root, error.message ?: "No fue posible consultar.", Snackbar.LENGTH_LONG).show()
                }
            binding.progressSeguimiento.visibility = View.GONE
            binding.buttonConsultar.isEnabled = true
        }
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
    }
}
