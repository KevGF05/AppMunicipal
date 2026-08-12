package com.example.appmunicipal.data.repository

import android.content.Context
import com.example.appmunicipal.data.remote.RetrofitClient
import com.example.appmunicipal.model.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException

class AppRepository(context: Context) {
    private val api = RetrofitClient.api(context)
    private val gson = Gson()
    private val textMediaType = "text/plain".toMediaType()
    private val imageMediaType = "image/jpeg".toMediaType()

    suspend fun crearReporte(
        tipo: String,
        descripcion: String,
        direccion: String,
        latitud: Double,
        longitud: Double,
        foto: File
    ): CreateReportResponse {
        val image = MultipartBody.Part.createFormData(
            "foto",
            foto.name,
            foto.asRequestBody(imageMediaType)
        )
        return api.crearReporte(
            tipo.toRequestBody(textMediaType),
            descripcion.toRequestBody(textMediaType),
            direccion.toRequestBody(textMediaType),
            latitud.toString().toRequestBody(textMediaType),
            longitud.toString().toRequestBody(textMediaType),
            image
        ).requireBody()
    }

    suspend fun consultar(codigo: String): TrackingResponse =
        api.consultarReporte(codigo).requireBody()


    suspend fun tiposReportePublicos(): List<ReportTypeResponse> =
        api.listarTiposReportePublicos().requireBody()

    suspend fun login(email: String, password: String): LoginResponse =
        api.login(LoginRequest(email, password)).requireBody()

    suspend fun cambiarPassword(nueva: String, confirmacion: String): MessageResponse =
        api.cambiarPassword(PasswordChangeRequest(nueva, confirmacion)).requireBody()

    suspend fun notificacionesNoLeidas(): List<NotificationResponse> =
        api.listarNotificaciones(soloNoLeidas = true).requireBody()

    suspend fun marcarNotificacionLeida(id: Long): NotificationResponse =
        api.marcarNotificacionLeida(id).requireBody()

    suspend fun listarAdmin(estado: String?): List<ReportResponse> =
        api.listarReportesAdmin(estado).requireBody()


    suspend fun tiposReporteAdmin(): List<ReportTypeResponse> =
        api.listarTiposReporteAdmin().requireBody()

    suspend fun crearTipoReporte(nombre: String): ReportTypeResponse =
        api.crearTipoReporte(ReportTypeCreateRequest(nombre)).requireBody()

    suspend fun obtenerAdmin(id: Long): ReportResponse = api.obtenerReporteAdmin(id).requireBody()
    suspend fun aprobar(id: Long): ReportResponse = api.aprobarReporte(id).requireBody()
    suspend fun rechazar(id: Long, motivo: String): ReportResponse =
        api.rechazarReporte(id, RejectRequest(motivo)).requireBody()

    suspend fun tecnicosDisponibles(): List<TechnicianResponse> =
        api.listarTecnicos(soloActivos = true).requireBody()

    suspend fun tecnicosAdmin(): List<TechnicianResponse> =
        api.listarTecnicos(soloActivos = false).requireBody()

    suspend fun crearTecnico(nombre: String, apellido: String, telefono: String): TechnicianResponse =
        api.crearTecnico(TechnicianCreateRequest(nombre, apellido, telefono)).requireBody()

    suspend fun editarTecnico(
        id: Long,
        nombre: String,
        apellido: String,
        telefono: String
    ): TechnicianResponse = api.editarTecnico(
        id,
        TechnicianUpdateRequest(nombre, apellido, telefono)
    ).requireBody()

    suspend fun cambiarEstadoTecnico(id: Long, activo: Boolean): TechnicianResponse =
        api.cambiarEstadoTecnico(id, TechnicianStatusRequest(activo)).requireBody()

    suspend fun resetPasswordTecnico(id: Long): TechnicianResponse =
        api.resetPasswordTecnico(id).requireBody()

    suspend fun asignar(
        id: Long,
        tecnicoId: Long,
        prioridad: String,
        observacion: String?
    ): ReportResponse = api.asignarReporte(
        id,
        AssignRequest(tecnicoId, prioridad, observacion)
    ).requireBody()

    suspend fun reasignar(
        id: Long,
        tecnicoId: Long,
        prioridad: String,
        observacion: String?
    ): ReportResponse = api.reasignarReporte(
        id,
        AssignRequest(tecnicoId, prioridad, observacion)
    ).requireBody()

    suspend fun listarTecnico(): List<ReportResponse> = api.listarReportesTecnico().requireBody()
    suspend fun obtenerTecnico(id: Long): ReportResponse = api.obtenerReporteTecnico(id).requireBody()
    suspend fun iniciar(id: Long): ReportResponse = api.iniciarReporte(id).requireBody()

    suspend fun cerrar(id: Long, comentario: String, foto: File): ReportResponse {
        val image = MultipartBody.Part.createFormData(
            "fotoFinal",
            foto.name,
            foto.asRequestBody(imageMediaType)
        )
        return api.cerrarReporte(
            id,
            comentario.toRequestBody(textMediaType),
            image
        ).requireBody()
    }

    private fun <T> Response<T>.requireBody(): T {
        if (isSuccessful) return body() ?: throw IOException("El servidor respondió sin datos.")
        val raw = errorBody()?.string()
        val message = runCatching {
            gson.fromJson(raw, ErrorResponse::class.java)?.message
        }.getOrNull().orEmpty()
        throw IOException(message.ifBlank { "Error HTTP ${code()}." })
    }
}
