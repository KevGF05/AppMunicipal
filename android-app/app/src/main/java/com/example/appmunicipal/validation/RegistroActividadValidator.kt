package com.example.appmunicipal.validation

data class RegistroActividadInput(
    val tipoTrabajo: String,
    val ubicacion: String,
    val latitud: Double?,
    val longitud: Double?,
    val detalle: String,
    val tieneFoto: Boolean,
    val aceptaProteccionDatos: Boolean
)

data class RegistroActividadErrors(
    val tipoTrabajo: String? = null,
    val ubicacion: String? = null,
    val detalle: String? = null,
    val foto: String? = null,
    val consentimiento: String? = null
) {
    val esValido: Boolean
        get() = tipoTrabajo == null &&
            ubicacion == null &&
            detalle == null &&
            foto == null &&
            consentimiento == null
}

object RegistroActividadValidator {

    fun validar(input: RegistroActividadInput): RegistroActividadErrors {
        return RegistroActividadErrors(
            tipoTrabajo = when {
                input.tipoTrabajo.isBlank() -> "Seleccione el tipo de reporte."
                input.tipoTrabajo.length > 80 -> "El tipo de reporte es demasiado largo."
                else -> null
            },
            ubicacion = when {
                input.latitud == null || input.longitud == null ->
                    "Seleccione la ubicación exacta en el mapa."
                !input.latitud.isFinite() || !input.longitud.isFinite() ->
                    "Las coordenadas seleccionadas no son válidas."
                input.ubicacion.isBlank() -> "Confirme la ubicación en el mapa."
                input.ubicacion.length > 300 ->
                    "La dirección no puede superar 300 caracteres."
                else -> null
            },
            detalle = when {
                input.detalle.isBlank() -> "Describa el problema reportado."
                input.detalle.length < 10 -> "El detalle debe contener al menos 10 caracteres."
                input.detalle.length > 500 -> "El detalle no puede superar 500 caracteres."
                else -> null
            },
            foto = if (!input.tieneFoto) {
                "Debe adjuntar una evidencia fotográfica."
            } else {
                null
            },
            consentimiento = if (!input.aceptaProteccionDatos) {
                "Debe confirmar la protección de datos."
            } else {
                null
            }
        )
    }
}
