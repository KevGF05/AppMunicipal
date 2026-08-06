package com.example.appmunicipal.ui.tecnico

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.local.TokenStore
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityTecnicoDashboardBinding
import com.example.appmunicipal.ui.InicioActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TecnicoDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTecnicoDashboardBinding
    private val repository by lazy { AppRepository(this) }
    private val tokenStore by lazy { TokenStore(this) }
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicoDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.textBienvenida.text = "Sesión: ${tokenStore.nombre()}"
        adapter = ReportAdapter { report ->
            startActivity(Intent(this, TecnicoDetalleActivity::class.java).putExtra(TecnicoDetalleActivity.EXTRA_ID, report.id))
        }
        binding.recyclerReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerReportes.adapter = adapter
        binding.buttonActualizar.setOnClickListener { cargar() }
        binding.buttonCerrarSesion.setOnClickListener { cerrarSesion() }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        binding.progressTecnico.visibility = View.VISIBLE
        binding.textVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarTecnico() }
                .onSuccess { reports ->
                    adapter.submitList(reports)
                    binding.textVacio.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure { error ->
                    Snackbar.make(binding.root, error.message ?: "No fue posible cargar los tickets.", Snackbar.LENGTH_LONG).show()
                }
            binding.progressTecnico.visibility = View.GONE
        }
    }

    private fun cerrarSesion() {
        tokenStore.limpiar()
        startActivity(Intent(this, InicioActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
