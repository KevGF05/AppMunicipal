package com.example.appmunicipal.util

object UiFormat {
    fun estado(value: String?): String = when (value) {
        "CREADO" -> "Creado"
        "APROBADO" -> "Aprobado"
        "RECHAZADO" -> "Rechazado"
        "ASIGNADO" -> "Asignado"
        "EN_PROCESO" -> "En proceso"
        "CERRADO" -> "Cerrado"
        else -> value.orEmpty()
    }

    fun prioridad(value: String?): String = when (value) {
        "BAJA" -> "Baja"
        "MEDIA" -> "Media"
        "ALTA" -> "Alta"
        "URGENTE" -> "Urgente"
        else -> value.orEmpty()
    }
}
