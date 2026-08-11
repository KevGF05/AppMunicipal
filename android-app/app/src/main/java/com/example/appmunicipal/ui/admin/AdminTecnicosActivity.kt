package com.example.appmunicipal.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmunicipal.data.repository.AppRepository
import com.example.appmunicipal.ui.auth.PersonalSessionActivity
import com.example.appmunicipal.databinding.ActivityAdminTecnicosBinding
import com.example.appmunicipal.databinding.DialogTecnicoBinding
import com.example.appmunicipal.model.TechnicianResponse
import com.example.appmunicipal.util.applySafeSystemInsets
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
        binding.root.applySafeSystemInsets()

        adapter = TechnicianAdapter(
            onEdit = { mostrarFormulario(it) },
            onStatus = { confirmarCambioEstado(it) },
            onResetPassword = { confirmarResetPassword(it) }
        )
        binding.recyclerTecnicos.layoutManager = LinearLayoutManager(this)
        binding.recyclerTecnicos.adapter = adapter

        val filtros = listOf("Activos", "Inactivos")
        binding.autoFiltroTecnicos.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filtros))
        binding.autoFiltroTecnicos.setText("Activos", false)
        binding.autoFiltroTecnicos.setOnItemClickListener { _, _, _, _ -> aplicarFiltro() }

        binding.buttonVolver.setOnClickListener { finish() }
        binding.buttonNuevoTecnico.setOnClickListener { mostrarFormulario(null) }
    }

    override fun onPersonalSessionResume() {
        if (::adapter.isInitialized) cargar()
    }

    private fun cargar() {
        binding.progressTecnicos.visibility = View.VISIBLE
        binding.textTecnicosVacio.visibility = View.GONE
        lifecycleScope.launch {
            runCatching { repository.tecnicosAdmin() }
                .onSuccess {
                    todos = it
                    aplicarFiltro()
                }
                .onFailure { mostrarError(it) }
            binding.progressTecnicos.visibility = View.GONE
        }
    }

    private fun aplicarFiltro() {
        val activos = binding.autoFiltroTecnicos.text.toString() != "Inactivos"
        val filtrados = todos.filter { it.active == activos }
        adapter.submitList(filtrados)
        binding.textTecnicosVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
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
                "Correo generado: pendiente"
            } else {
                "Correo generado: $correo"
            }
        }

        form.editNombreTecnico.doAfterTextChanged { actualizarCorreo() }
        form.editApellidoTecnico.doAfterTextChanged { actualizarCorreo() }
        actualizarCorreo()

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editando) "Editar técnico" else "Crear técnico")
            .setView(form.root)
            .setPositiveButton(if (editando) "Guardar" else "Crear", null)
            .setNegativeButton("Cancelar", null)
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
                    form.layoutNombreTecnico.error = "Ingrese el nombre del técnico."
                    valido = false
                }
                if (apellido.length < 2) {
                    form.layoutApellidoTecnico.error = "Ingrese el apellido del técnico."
                    valido = false
                }
                if (!telefono.matches(Regex("^[+0-9() -]{8,30}$"))) {
                    form.layoutTelefonoTecnico.error = "Ingrese un teléfono válido."
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
        binding.progressTecnicos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching {
                if (tecnico == null) {
                    repository.crearTecnico(nombre, apellido, telefono)
                } else {
                    repository.editarTecnico(tecnico.id, nombre, apellido, telefono)
                }
            }.onSuccess { actualizado ->
                val mensaje = if (tecnico == null) {
                    "Técnico creado. Correo: ${actualizado.email}. Contraseña temporal: Tecnico123*"
                } else {
                    "Técnico actualizado. Correo: ${actualizado.email}"
                }
                mostrarMensaje(mensaje)
                cargar()
            }.onFailure { mostrarError(it) }
            binding.progressTecnicos.visibility = View.GONE
        }
    }

    private fun confirmarCambioEstado(tecnico: TechnicianResponse) {
        val activar = !tecnico.active
        AlertDialog.Builder(this)
            .setTitle(if (activar) "Activar técnico" else "Desactivar técnico")
            .setMessage(
                if (activar) {
                    "${tecnico.name} podrá volver a iniciar sesión y recibir asignaciones."
                } else {
                    "${tecnico.name} no podrá iniciar sesión ni recibir nuevas asignaciones."
                }
            )
            .setPositiveButton(if (activar) "Activar" else "Desactivar") { _, _ ->
                cambiarEstado(tecnico, activar)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarEstado(tecnico: TechnicianResponse, activo: Boolean) {
        binding.progressTecnicos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.cambiarEstadoTecnico(tecnico.id, activo) }
                .onSuccess {
                    mostrarMensaje(if (activo) "Técnico activado." else "Técnico desactivado.")
                    cargar()
                }
                .onFailure { mostrarError(it) }
            binding.progressTecnicos.visibility = View.GONE
        }
    }

    private fun confirmarResetPassword(tecnico: TechnicianResponse) {
        AlertDialog.Builder(this)
            .setTitle("Restablecer contraseña")
            .setMessage("La contraseña de ${tecnico.name} volverá a ser Tecnico123*. Se cerrará su sesión y deberá crear una contraseña nueva al ingresar.")
            .setPositiveButton("Restablecer") { _, _ -> resetPassword(tecnico) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun resetPassword(tecnico: TechnicianResponse) {
        binding.progressTecnicos.visibility = View.VISIBLE
        lifecycleScope.launch {
            runCatching { repository.resetPasswordTecnico(tecnico.id) }
                .onSuccess {
                    mostrarMensaje("Contraseña restablecida a Tecnico123*.")
                    cargar()
                }
                .onFailure { mostrarError(it) }
            binding.progressTecnicos.visibility = View.GONE
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

    private fun mostrarError(error: Throwable) = mostrarMensaje(error.message ?: "No fue posible completar la acción.")
    private fun mostrarMensaje(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
}
