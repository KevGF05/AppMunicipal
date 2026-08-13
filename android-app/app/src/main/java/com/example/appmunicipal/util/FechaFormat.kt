package com.example.appmunicipal.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * El backend devuelve las fechas en formato ISO (2026-08-12T14:05:33.123).
 * Mostrarlas tal cual es poco legible, asi que aqui se convierten a texto corto.
 */
object FechaFormat {

    private val ESPANOL = Locale("es", "HN")

    private val formatoSalida = SimpleDateFormat("d 'de' MMMM, HH:mm", ESPANOL)
    private val formatoCorto = SimpleDateFormat("dd/MM/yyyy HH:mm", ESPANOL)

    /** Convierte una marca de tiempo ISO del backend a texto legible. */
    fun desdeIso(valor: String?): String? {
        if (valor.isNullOrBlank()) return null
        val limpio = valor.replace("T", " ").substringBefore('.')
        val patrones = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")
        for (patron in patrones) {
            val fecha = runCatching { SimpleDateFormat(patron, ESPANOL).parse(limpio) }.getOrNull()
            if (fecha != null) return formatoSalida.format(fecha)
        }
        return limpio
    }

    /** Convierte milisegundos guardados en la base local a texto legible. */
    fun desdeMillis(valor: Long): String = formatoCorto.format(Date(valor))
}
