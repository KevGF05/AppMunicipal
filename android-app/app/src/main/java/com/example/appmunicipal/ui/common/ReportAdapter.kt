package com.example.appmunicipal.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.databinding.ItemReporteBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.util.UiFormat

/**
 * Antes era un RecyclerView.Adapter con una lista mutable y notifyDataSetChanged(),
 * que redibuja todo y pierde la posicion del scroll en cada actualizacion.
 * Con ListAdapter y DiffUtil solo se redibuja lo que cambio y las animaciones salen solas.
 */
class ReportAdapter(
    private val onClick: (ReportResponse) -> Unit
) : ListAdapter<ReportResponse, ReportAdapter.ReportViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReporteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReportViewHolder(
        private val binding: ItemReporteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReportResponse) {
            binding.textCodigo.text = item.code
            binding.textTipo.text = item.type
            binding.textDireccion.text = item.address
            UiFormat.aplicarEstado(binding.textEstado, item.status)
            UiFormat.aplicarPrioridad(binding.textPrioridad, item.priority)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ReportResponse>() {
            override fun areItemsTheSame(oldItem: ReportResponse, newItem: ReportResponse) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ReportResponse, newItem: ReportResponse) =
                oldItem == newItem
        }
    }
}
