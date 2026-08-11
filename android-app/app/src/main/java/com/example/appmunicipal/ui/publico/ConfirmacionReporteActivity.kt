package com.example.appmunicipal.ui.publico

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appmunicipal.databinding.ActivityConfirmacionReporteBinding
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.util.UiFormat
import com.example.appmunicipal.util.applySafeSystemInsets

class ConfirmacionReporteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmacionReporteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmacionReporteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        val code = intent.getStringExtra(EXTRA_CODIGO).orEmpty()
        val status = intent.getStringExtra(EXTRA_ESTADO).orEmpty()
        binding.textCodigo.text = code
        binding.textEstado.text = UiFormat.estado(status)

        binding.buttonConsultar.setOnClickListener {
            startActivity(Intent(this, SeguimientoActivity::class.java).apply {
                putExtra(SeguimientoActivity.EXTRA_CODIGO, code)
            })
        }
        binding.buttonInicio.setOnClickListener {
            startActivity(Intent(this, InicioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_ESTADO = "estado"
    }
}
