package com.example.appmunicipal.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.databinding.ItemReportTypeBinding
import com.example.appmunicipal.model.ReportTypeResponse

class ReportTypeAdapter : ListAdapter<ReportTypeResponse, ReportTypeAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(private val binding: ItemReportTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReportTypeResponse, position: Int) {
            binding.textNumeroTipo.text = position.toString()
            binding.textNombreTipo.text = item.name
            binding.textEstadoTipo.text = if (item.active) "Disponible" else "Inactivo"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReportTypeResponse>() {
            override fun areItemsTheSame(oldItem: ReportTypeResponse, newItem: ReportTypeResponse) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ReportTypeResponse, newItem: ReportTypeResponse) = oldItem == newItem
        }
    }
}
