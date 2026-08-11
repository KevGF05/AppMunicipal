package com.example.appmunicipal.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.R
import com.example.appmunicipal.databinding.ItemTecnicoBinding
import com.example.appmunicipal.model.TechnicianResponse

class TechnicianAdapter(
    private val onEdit: (TechnicianResponse) -> Unit,
    private val onStatus: (TechnicianResponse) -> Unit,
    private val onResetPassword: (TechnicianResponse) -> Unit
) : ListAdapter<TechnicianResponse, TechnicianAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTecnicoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTecnicoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TechnicianResponse) {
            binding.textNombreTecnico.text = item.name
            binding.textCorreoTecnico.text = item.email
            binding.textTelefonoTecnico.text = item.phoneNumber?.takeIf { it.isNotBlank() } ?: "Teléfono pendiente"
            binding.textEstadoTecnico.text = if (item.active) "Activo" else "Inactivo"
            binding.textEstadoTecnico.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (item.active) R.color.municipal_success else R.color.municipal_danger
                )
            )
            binding.textPasswordPendiente.visibility = if (item.passwordChangeRequired) View.VISIBLE else View.GONE
            binding.buttonEstadoTecnico.text = if (item.active) "Desactivar" else "Activar"
            binding.buttonEditarTecnico.setOnClickListener { onEdit(item) }
            binding.buttonEstadoTecnico.setOnClickListener { onStatus(item) }
            binding.buttonResetPassword.setOnClickListener { onResetPassword(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TechnicianResponse>() {
        override fun areItemsTheSame(oldItem: TechnicianResponse, newItem: TechnicianResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TechnicianResponse, newItem: TechnicianResponse) = oldItem == newItem
    }
}
