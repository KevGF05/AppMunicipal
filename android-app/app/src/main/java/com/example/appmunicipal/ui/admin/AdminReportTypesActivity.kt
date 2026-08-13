package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.R
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminReportTypesBinding
import com.example.appmunicipal.databinding.DialogReportTypeBinding
import com.example.appmunicipal.model.ReportTypeResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AdminReportTypesActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminReportTypesBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: ReportTypeAdapter
    private var allTypes: List<ReportTypeResponse> = emptyList()
    private var showingInactive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminReportTypesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        adapter = ReportTypeAdapter(
            onEdit = { showTypeForm(it) },
            onToggle = { confirmStatusChange(it) }
        )
        binding.recyclerTiposReporte.layoutManager = LinearLayoutManager(this)
        binding.recyclerTiposReporte.adapter = adapter

        binding.buttonAgregarTipo.setOnClickListener { showTypeForm(null) }
        binding.buttonVerDesactivados.setOnClickListener {
            showingInactive = !showingInactive
            renderCurrentList()
        }
        binding.swipeRefresh.setOnRefreshListener { loadTypes() }

        binding.recyclerTiposReporte.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8) binding.buttonAgregarTipo.shrink()
                else if (dy < -8) binding.buttonAgregarTipo.extend()
            }
        })
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) loadTypes()
    }

    private fun loadTypes() {
        binding.swipeRefresh.isRefreshing = true
        binding.layoutVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.tiposReporteAdmin() }
                .onSuccess {
                    allTypes = it
                    renderCurrentList()
                }
                .onFailure { showError(it) }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun renderCurrentList() {
        val activeCount = allTypes.count { it.active }
        val inactiveCount = allTypes.size - activeCount
        val visible = allTypes.filter { if (showingInactive) !it.active else it.active }

        adapter.submitList(visible)
        binding.textSeccionTipos.setText(
            if (showingInactive) R.string.tipos_seccion_inactivos else R.string.tipos_seccion_activos
        )
        binding.textCantidadTipos.text = if (showingInactive) {
            getString(R.string.tipos_inactivos_cantidad, inactiveCount)
        } else {
            getString(R.string.tipos_activos_cantidad, activeCount)
        }
        binding.buttonVerDesactivados.text = if (showingInactive) {
            getString(R.string.tipos_ver_activos, activeCount)
        } else {
            getString(R.string.tipos_ver_desactivados, inactiveCount)
        }
        binding.textVacioDetalle.setText(
            if (showingInactive) R.string.tipos_vacio_inactivos else R.string.tipos_vacio_activos
        )
        binding.layoutVacio.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showTypeForm(type: ReportTypeResponse?) {
        val dialogBinding = DialogReportTypeBinding.inflate(layoutInflater)
        val editing = type != null

        if (editing) {
            dialogBinding.editNombreTipo.setText(type?.name.orEmpty())
            dialogBinding.editNombreTipo.selectAll()
            dialogBinding.textAyudaTipo.setText(R.string.tipos_ayuda_editar)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editing) R.string.tipos_editar else R.string.tipos_agregar)
            .setView(dialogBinding.root)
            .setPositiveButton(if (editing) R.string.accion_guardar else R.string.accion_agregar, null)
            .setNegativeButton(R.string.accion_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.layoutNombreTipo.error = null
                val name = normalize(dialogBinding.editNombreTipo.text?.toString())
                if (!validateName(name) { dialogBinding.layoutNombreTipo.error = it }) {
                    return@setOnClickListener
                }
                dialog.dismiss()
                if (editing) updateType(type!!.id, name) else createType(name)
            }
        }
        dialog.show()
        dialogBinding.editNombreTipo.requestFocus()
    }

    private fun createType(name: String) {
        binding.swipeRefresh.isRefreshing = true
        binding.buttonAgregarTipo.isEnabled = false
        lifecycleScope.launch {
            runCatching { repository.crearTipoReporte(name) }
                .onSuccess {
                    showingInactive = false
                    showMessage(getString(R.string.tipos_agregado))
                    loadTypes()
                }
                .onFailure {
                    showError(it)
                    binding.swipeRefresh.isRefreshing = false
                }
            binding.buttonAgregarTipo.isEnabled = true
        }
    }

    private fun updateType(id: Long, name: String) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            runCatching { repository.editarTipoReporte(id, name) }
                .onSuccess {
                    showMessage(getString(R.string.tipos_actualizado))
                    loadTypes()
                }
                .onFailure {
                    showError(it)
                    binding.swipeRefresh.isRefreshing = false
                }
        }
    }

    private fun confirmStatusChange(type: ReportTypeResponse) {
        val activate = !type.active
        AlertDialog.Builder(this)
            .setTitle(if (activate) R.string.tipos_activar_titulo else R.string.tipos_desactivar_titulo)
            .setMessage(
                getString(
                    if (activate) R.string.tipos_activar_mensaje else R.string.tipos_desactivar_mensaje,
                    type.name
                )
            )
            .setPositiveButton(if (activate) R.string.accion_activar else R.string.accion_desactivar) { _, _ ->
                changeStatus(type.id, activate)
            }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    private fun changeStatus(id: Long, active: Boolean) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            runCatching { repository.cambiarEstadoTipoReporte(id, active) }
                .onSuccess {
                    showMessage(getString(if (active) R.string.tipos_activado else R.string.tipos_desactivado))
                    loadTypes()
                }
                .onFailure {
                    showError(it)
                    binding.swipeRefresh.isRefreshing = false
                }
        }
    }

    private fun normalize(value: String?): String = value.orEmpty().trim().replace(Regex("\\s+"), " ")

    private fun validateName(name: String, setError: (CharSequence?) -> Unit): Boolean {
        return when {
            name.length < 3 -> { setError(getString(R.string.tipos_min)); false }
            name.length > 80 -> { setError(getString(R.string.tipos_max)); false }
            else -> true
        }
    }

    private fun showError(error: Throwable) =
        showMessage(error.message ?: getString(R.string.detalle_accion_error))

    private fun showMessage(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
}
