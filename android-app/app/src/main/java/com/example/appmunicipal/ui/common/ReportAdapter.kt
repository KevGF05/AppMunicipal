package com.example.appmunicipal.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.databinding.ItemReporteBinding
import com.example.appmunicipal.model.ReportResponse
import com.example.appmunicipal.util.UiFormat

class ReportAdapter(
    private val onClick: (ReportResponse) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private val items = mutableListOf<ReportResponse>()

    fun submitList(values: List<ReportResponse>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReporteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ReportViewHolder(
        private val binding: ItemReporteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReportResponse) {
            binding.textCodigo.text = item.code
            binding.textTipo.text = item.type
            binding.textEstado.text = UiFormat.estado(item.status)
            binding.textPrioridad.text = "Prioridad: ${UiFormat.prioridad(item.priority)}"
            binding.textDireccion.text = item.address
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
