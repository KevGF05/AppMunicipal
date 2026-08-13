package com.example.appmunicipal.ui.tecnico

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.R
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityTecnicoHistorialBinding
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.ui.common.ReportAdapter
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TecnicoHistorialActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityTecnicoHistorialBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicoHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        adapter = ReportAdapter { report ->
            startActivity(
                Intent(this, TecnicoDetalleActivity::class.java)
                    .putExtra(TecnicoDetalleActivity.EXTRA_ID, report.id)
            )
        }
        binding.recyclerHistorial.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistorial.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { cargar() }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.swipeRefresh.isRefreshing = true
        binding.layoutVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.listarTecnico() }
                .onSuccess { values ->
                    val cerrados = values.filter { it.status == "CERRADO" }
                    adapter.submitList(cerrados)
                    binding.toolbar.subtitle = resources.getQuantityString(
                        R.plurals.trabajos_completados,
                        cerrados.size,
                        cerrados.size
                    )
                    binding.layoutVacio.visibility = if (cerrados.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure {
                    Snackbar.make(
                        binding.root,
                        it.message ?: getString(R.string.tecnico_historial_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            binding.swipeRefresh.isRefreshing = false
        }
    }
}
