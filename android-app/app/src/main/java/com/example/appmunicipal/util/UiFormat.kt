package com.example.appmunicipal.util

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.example.appmunicipal.R

/**
 * Traduce los estados y prioridades del backend a texto y color.
 *
 * Antes solo devolvia texto y la lista se veia como un bloque gris. Ahora tambien
 * pinta la etiqueta, que es lo que permite reconocer el estado de un ticket sin leerlo.
 */
object UiFormat {

    // ---------- Texto ----------

    fun estado(context: Context, value: String?): String = when (value) {
        "CREADO" -> context.getString(R.string.estado_creado)
        "APROBADO" -> context.getString(R.string.estado_aprobado)
        "RECHAZADO" -> context.getString(R.string.estado_rechazado)
        "ASIGNADO" -> context.getString(R.string.estado_asignado)
        "EN_PROCESO" -> context.getString(R.string.estado_en_proceso)
        "PAUSADO" -> context.getString(R.string.estado_pausado)
        "CERRADO" -> context.getString(R.string.estado_cerrado)
        else -> value.orEmpty()
    }

    fun prioridad(context: Context, value: String?): String = when (value) {
        "BAJA" -> context.getString(R.string.prioridad_baja)
        "MEDIA" -> context.getString(R.string.prioridad_media)
        "ALTA" -> context.getString(R.string.prioridad_alta)
        "URGENTE" -> context.getString(R.string.prioridad_urgente)
        else -> value.orEmpty()
    }

    // ---------- Etiquetas de color ----------

    /** Pinta el TextView como etiqueta del estado indicado. */
    fun aplicarEstado(textView: TextView, value: String?) {
        val (fondo, texto) = when (value) {
            "CREADO" -> R.color.estado_creado_bg to R.color.estado_creado_fg
            "APROBADO" -> R.color.estado_aprobado_bg to R.color.estado_aprobado_fg
            "RECHAZADO" -> R.color.estado_rechazado_bg to R.color.estado_rechazado_fg
            "ASIGNADO" -> R.color.estado_asignado_bg to R.color.estado_asignado_fg
            "EN_PROCESO" -> R.color.estado_proceso_bg to R.color.estado_proceso_fg
            "PAUSADO" -> R.color.estado_pausado_bg to R.color.estado_pausado_fg
            "CERRADO" -> R.color.estado_cerrado_bg to R.color.estado_cerrado_fg
            else -> R.color.estado_cerrado_bg to R.color.estado_cerrado_fg
        }
        pintar(textView, estado(textView.context, value), fondo, texto)
    }

    /** Pinta el TextView como etiqueta de prioridad. */
    fun aplicarPrioridad(textView: TextView, value: String?) {
        val (fondo, texto) = when (value) {
            "BAJA" -> R.color.prioridad_baja_bg to R.color.prioridad_baja_fg
            "MEDIA" -> R.color.prioridad_media_bg to R.color.prioridad_media_fg
            "ALTA" -> R.color.prioridad_alta_bg to R.color.prioridad_alta_fg
            "URGENTE" -> R.color.prioridad_urgente_bg to R.color.prioridad_urgente_fg
            else -> R.color.prioridad_baja_bg to R.color.prioridad_baja_fg
        }
        val etiqueta = textView.context.getString(
            R.string.detalle_prioridad,
            prioridad(textView.context, value)
        )
        pintar(textView, etiqueta, fondo, texto)
    }

    /** Etiqueta generica de dos estados, por ejemplo activo e inactivo. */
    fun aplicarActivo(textView: TextView, activo: Boolean, textoActivo: String, textoInactivo: String) {
        val (fondo, texto) = if (activo) {
            R.color.estado_aprobado_bg to R.color.estado_aprobado_fg
        } else {
            R.color.estado_cerrado_bg to R.color.estado_cerrado_fg
        }
        pintar(textView, if (activo) textoActivo else textoInactivo, fondo, texto)
    }

    private fun pintar(textView: TextView, texto: String, colorFondo: Int, colorTexto: Int) {
        textView.text = texto
        ViewCompat.setBackgroundTintList(
            textView,
            ColorStateList.valueOf(ContextCompat.getColor(textView.context, colorFondo))
        )
        textView.setTextColor(ContextCompat.getColor(textView.context, colorTexto))
    }
}
