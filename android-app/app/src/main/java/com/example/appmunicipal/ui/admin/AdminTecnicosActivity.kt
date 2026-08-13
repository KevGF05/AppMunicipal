package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.R
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.databinding.ActivityAdminTecnicosBinding
import com.example.appmunicipal.databinding.DialogTecnicoBinding
import com.example.appmunicipal.model.TechnicianResponse
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.util.applySafeSystemInsets
import com.example.appmunicipal.util.configurarToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

class AdminTecnicosActivity : PersonalSessionActivity() {
    private lateinit var binding: ActivityAdminTecnicosBinding
    private val repository by lazy { AppRepository(this) }
    private lateinit var adapter: TechnicianAdapter
    private var todos: List<TechnicianResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminTecnicosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets(topAnchor = binding.toolbar)
        configurarToolbar(binding.toolbar)

        adapter = TechnicianAdapter(
            onEdit = { mostrarFormulario(it) },
            onStatus = { confirmarCambioEstado(it) },
            onResetPassword = { confirmarResetPassword(it) }
        )
        binding.recyclerTecnicos.layoutManager = LinearLayoutManager(this)
        binding.recyclerTecnicos.adapter = adapter

        binding.chipGroupTecnicos.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) group.check(R.id.chipActivos) else aplicarFiltro()
        }

        binding.swipeRefresh.setOnRefreshListener { cargar() }
        binding.buttonNuevoTecnico.setOnClickListener { mostrarFormulario(null) }

        // El boton flotante se encoge al desplazar la lista para no tapar la ultima tarjeta.
        binding.recyclerTecnicos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8) binding.buttonNuevoTecnico.shrink()
                else if (dy < -8) binding.buttonNuevoTecnico.extend()
            }
        })
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.swipeRefresh.isRefreshing = true
        binding.layoutVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.tecnicosAdmin() }
                .onSuccess {
                    todos = it
                    aplicarFiltro()
                }
                .onFailure { mostrarError(it) }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun aplicarFiltro() {
        val activos = binding.chipGroupTecnicos.checkedChipId != R.id.chipInactivos
        val filtrados = todos.filter { it.active == activos }
        adapter.submitList(filtrados)
        binding.layoutVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarFormulario(tecnico: TechnicianResponse?) {
        val form = DialogTecnicoBinding.inflate(layoutInflater)
        val editando = tecnico != null

        if (tecnico != null) {
            val partes = partesNombre(tecnico)
            form.editNombreTecnico.setText(partes.first)
            form.editApellidoTecnico.setText(partes.second)
            form.editTelefonoTecnico.setText(tecnico.phoneNumber.orEmpty())
        }

        fun actualizarCorreo() {
            val nombre = form.editNombreTecnico.text?.toString()?.trim().orEmpty()
            val apellido = form.editApellidoTecnico.text?.toString()?.trim().orEmpty()
            val correo = correoVistaPrevia(nombre, apellido)
            form.textCorreoGenerado.text = if (correo == null) {
                getString(R.string.tecnicos_correo_pendiente)
            } else {
                getString(R.string.tecnicos_correo_generado, correo)
            }
        }

        form.editNombreTecnico.doAfterTextChanged { actualizarCorreo() }
        form.editApellidoTecnico.doAfterTextChanged { actualizarCorreo() }
        actualizarCorreo()

        // La contrasena temporal solo aplica a cuentas nuevas.
        form.textPasswordTecnico.visibility = if (editando) View.GONE else View.VISIBLE

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editando) R.string.tecnicos_editar else R.string.tecnicos_crear)
            .setView(form.root)
            .setPositiveButton(if (editando) R.string.accion_guardar else R.string.accion_crear, null)
            .setNegativeButton(R.string.accion_cancelar, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                form.layoutNombreTecnico.error = null
                form.layoutApellidoTecnico.error = null
                form.layoutTelefonoTecnico.error = null

                val nombre = form.editNombreTecnico.text?.toString()?.trim().orEmpty()
                val apellido = form.editApellidoTecnico.text?.toString()?.trim().orEmpty()
                val telefono = form.editTelefonoTecnico.text?.toString()?.trim().orEmpty()

                var valido = true
                if (nombre.length < 2) {
                    form.layoutNombreTecnico.error = getString(R.string.tecnicos_falta_nombre)
                    valido = false
                }
                if (apellido.length < 2) {
                    form.layoutApellidoTecnico.error = getString(R.string.tecnicos_falta_apellido)
                    valido = false
                }
                if (!telefono.matches(Regex("^[+0-9() -]{8,30}$"))) {
                    form.layoutTelefonoTecnico.error = getString(R.string.tecnicos_falta_telefono)
                    valido = false
                }
                if (!valido) return@setOnClickListener

                dialog.dismiss()
                guardar(tecnico, nombre, apellido, telefono)
            }
        }
        dialog.show()
    }

    private fun guardar(tecnico: TechnicianResponse?, nombre: String, apellido: String, telefono: String) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            runCatching {
                if (tecnico == null) {
                    repository.crearTecnico(nombre, apellido, telefono)
                } else {
                    repository.editarTecnico(tecnico.id, nombre, apellido, telefono)
                }
            }.onSuccess { actualizado ->
                val mensaje = if (tecnico == null) {
                    getString(R.string.tecnicos_creado, actualizado.email)
                } else {
                    getString(R.string.tecnicos_actualizado, actualizado.email)
                }
                mostrarMensaje(mensaje)
                cargar()
            }.onFailure {
                mostrarError(it)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmarCambioEstado(tecnico: TechnicianResponse) {
        val activar = !tecnico.active
        AlertDialog.Builder(this)
            .setTitle(if (activar) R.string.tecnicos_activar_titulo else R.string.tecnicos_desactivar_titulo)
            .setMessage(
                getString(
                    if (activar) R.string.tecnicos_activar_mensaje else R.string.tecnicos_desactivar_mensaje,
                    tecnico.name
                )
            )
            .setPositiveButton(if (activar) R.string.accion_activar else R.string.accion_desactivar) { _, _ ->
                cambiarEstado(tecnico, activar)
            }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    private fun cambiarEstado(tecnico: TechnicianResponse, activo: Boolean) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            runCatching { repository.cambiarEstadoTecnico(tecnico.id, activo) }
                .onSuccess {
                    mostrarMensaje(
                        getString(if (activo) R.string.tecnicos_activado else R.string.tecnicos_desactivado)
                    )
                    cargar()
                }
                .onFailure {
                    mostrarError(it)
                    binding.swipeRefresh.isRefreshing = false
                }
        }
    }

    private fun confirmarResetPassword(tecnico: TechnicianResponse) {
        AlertDialog.Builder(this)
            .setTitle(R.string.tecnicos_reset_titulo)
            .setMessage(getString(R.string.tecnicos_reset_mensaje, tecnico.name))
            .setPositiveButton(R.string.tecnicos_reset_titulo) { _, _ -> resetPassword(tecnico) }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    private fun resetPassword(tecnico: TechnicianResponse) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            runCatching { repository.resetPasswordTecnico(tecnico.id) }
                .onSuccess {
                    mostrarMensaje(getString(R.string.tecnicos_reset_ok))
                    cargar()
                }
                .onFailure {
                    mostrarError(it)
                    binding.swipeRefresh.isRefreshing = false
                }
        }
    }

    private fun partesNombre(tecnico: TechnicianResponse): Pair<String, String> {
        val first = tecnico.firstName?.trim().orEmpty()
        val last = tecnico.lastName?.trim().orEmpty()
        if (first.isNotBlank() && last.isNotBlank()) return first to last

        val pieces = tecnico.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            pieces.size >= 2 -> pieces.first() to pieces.last()
            pieces.size == 1 -> pieces.first() to "Municipal"
            else -> "" to ""
        }
    }

    private fun correoVistaPrevia(nombre: String, apellido: String): String? {
        val first = normalizarParaCorreo(nombre)
        val last = normalizarParaCorreo(apellido)
        if (first.isBlank() || last.isBlank()) return null
        return "$first${last.first()}@pmunicipalidad.hn"
    }

    private fun normalizarParaCorreo(value: String): String {
        val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
        return withoutAccents.replace(Regex("[^a-z0-9]"), "")
    }

    private fun mostrarError(error: Throwable) =
        mostrarMensaje(error.message ?: getString(R.string.detalle_accion_error))

    private fun mostrarMensaje(message: String) =
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
}
