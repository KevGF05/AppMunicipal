package com.example.appmunicipal.ui.publico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmunicipal.R
import com.example.appmunicipal.data.local.SeguimientoLocal
import com.example.appmunicipal.databinding.ItemMiReporteBinding
import com.example.appmunicipal.util.FechaFormat
import com.example.appmunicipal.util.UiFormat

class MisReportesAdapter(
    private val onClick: (SeguimientoLocal) -> Unit,
    private val onCopiar: (SeguimientoLocal) -> Unit,
    private val onQuitar: (SeguimientoLocal) -> Unit
) : ListAdapter<SeguimientoLocal, MisReportesAdapter.MiReporteViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiReporteViewHolder {
        val binding = ItemMiReporteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MiReporteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MiReporteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MiReporteViewHolder(
        private val binding: ItemMiReporteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SeguimientoLocal) {
            val context = binding.root.context
            binding.textCodigo.text = item.codigo
            binding.textTipo.text = item.tipo.orEmpty()
            binding.textTipo.visibility = if (item.tipo.isNullOrBlank()) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
            UiFormat.aplicarEstado(binding.textEstado, item.estado)
            binding.textFecha.text = context.getString(
                R.string.mis_reportes_consultado,
                FechaFormat.desdeMillis(item.fecha)
            )

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onQuitar(item)
                true
            }
            binding.buttonCopiarCodigo.setOnClickListener { onCopiar(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<SeguimientoLocal>() {
            override fun areItemsTheSame(oldItem: SeguimientoLocal, newItem: SeguimientoLocal) =
                oldItem.codigo == newItem.codigo

            override fun areContentsTheSame(oldItem: SeguimientoLocal, newItem: SeguimientoLocal) =
                oldItem == newItem
        }
    }
}
