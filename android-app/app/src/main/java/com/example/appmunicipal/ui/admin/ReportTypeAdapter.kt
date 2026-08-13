package com.example.appmunicipal.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.R
import com.example.appmunicipal.databinding.ItemReportTypeBinding
import com.example.appmunicipal.model.ReportTypeResponse
import com.example.appmunicipal.util.UiFormat

class ReportTypeAdapter(
    private val onEdit: (ReportTypeResponse) -> Unit,
    private val onToggle: (ReportTypeResponse) -> Unit
) : ListAdapter<ReportTypeResponse, ReportTypeAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemReportTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReportTypeResponse, position: Int) {
            val context = binding.root.context
            binding.textNumeroTipo.text = position.toString()
            binding.textNombreTipo.text = item.name

            UiFormat.aplicarActivo(
                binding.textEstadoTipo,
                item.active,
                context.getString(R.string.tipos_disponible),
                context.getString(R.string.tipos_inactivo)
            )

            binding.buttonEditarTipo.setOnClickListener { onEdit(item) }
            binding.buttonEstadoTipo.text = context.getString(
                if (item.active) R.string.accion_desactivar else R.string.accion_activar
            )
            binding.buttonEstadoTipo.setOnClickListener { onToggle(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReportTypeResponse>() {
            override fun areItemsTheSame(oldItem: ReportTypeResponse, newItem: ReportTypeResponse) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ReportTypeResponse, newItem: ReportTypeResponse) = oldItem == newItem
        }
    }
}
