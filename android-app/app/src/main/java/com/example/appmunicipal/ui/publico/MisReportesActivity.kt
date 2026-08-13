package com.example.appmunicipal.ui.publico

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.SeguimientoDbHelper
import com.example.appmunicipal.data.local.SeguimientoLocal
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityMisReportesBinding
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla nueva: la lista de codigos enviados desde este telefono.
 *
 * La tabla local ya guardaba todos los codigos, pero la app solo consultaba el
 * ultimo. Con esta pantalla el ciudadano deja de depender de haber anotado el
 * codigo en algun lado.
 *
 * Al abrirla se muestran de inmediato los datos guardados y despues se consulta el
 * backend para refrescar cada estado. Si no hay conexion, se conserva lo guardado.
 */
class MisReportesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMisReportesBinding
    private lateinit var adapter: MisReportesAdapter
    private val database by lazy { SeguimientoDbHelper(this) }
    private val repository by lazy { AppRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMisReportesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        adapter = MisReportesAdapter(
            onClick = { abrirSeguimiento(it) },
            onCopiar = { copiarCodigo(it) },
            onQuitar = { confirmarQuitar(it) }
        )
        binding.recyclerMisReportes.layoutManager = LinearLayoutManager(this)
        binding.recyclerMisReportes.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refrescarEstados() }
        binding.buttonCrearReporte.setOnClickListener {
            startActivity(Intent(this, RegistroActividadActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        mostrarGuardados()
        refrescarEstados()
    }

    private fun mostrarGuardados() {
        val guardados = database.todos()
        adapter.submitList(guardados)
        binding.layoutVacio.visibility = if (guardados.isEmpty()) View.VISIBLE else View.GONE
        binding.textResumen.visibility = if (guardados.isEmpty()) View.GONE else View.VISIBLE
        binding.textResumen.text = resources.getQuantityString(
            R.plurals.reportes_guardados,
            guardados.size,
            guardados.size
        )
    }

    /** Consulta el backend para actualizar el estado de cada codigo guardado. */
    private fun refrescarEstados() {
        val guardados = database.todos()
        if (guardados.isEmpty()) {
            binding.swipeRefresh.isRefreshing = false
            return
        }

        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            var huboError = false
            guardados.forEach { local ->
                runCatching { repository.consultar(local.codigo) }
                    .onSuccess { database.guardar(it.code, it.status) }
                    .onFailure { huboError = true }
            }
            mostrarGuardados()
            binding.swipeRefresh.isRefreshing = false
            if (huboError) {
                Snackbar.make(binding.root, R.string.mis_reportes_sin_conexion, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun abrirSeguimiento(item: SeguimientoLocal) {
        startActivity(
            Intent(this, SeguimientoActivity::class.java)
                .putExtra(SeguimientoActivity.EXTRA_CODIGO, item.codigo)
        )
    }

    private fun copiarCodigo(item: SeguimientoLocal) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.confirmacion_label_codigo), item.codigo)
        )
        Snackbar.make(binding.root, R.string.confirmacion_copiado, Snackbar.LENGTH_SHORT).show()
    }

    private fun confirmarQuitar(item: SeguimientoLocal) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mis_reportes_quitar_titulo)
            .setMessage(getString(R.string.mis_reportes_quitar_mensaje, item.codigo))
            .setPositiveButton(R.string.accion_quitar) { _, _ ->
                database.eliminar(item.codigo)
                mostrarGuardados()
                Snackbar.make(binding.root, R.string.mis_reportes_quitado, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }
}
