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

    suspend fun consultar(codigo: String, token: String): TrackingResponse =
        api.consultarReporte(codigo, token).requireBody()

    suspend fun login(email: String, password: String): LoginResponse =
        api.login(LoginRequest(email, password)).requireBody()

    suspend fun listarAdmin(estado: String?): List<ReportResponse> =
        api.listarReportesAdmin(estado).requireBody()

    suspend fun obtenerAdmin(id: Long): ReportResponse = api.obtenerReporteAdmin(id).requireBody()
    suspend fun aprobar(id: Long): ReportResponse = api.aprobarReporte(id).requireBody()
    suspend fun rechazar(id: Long, motivo: String): ReportResponse =
        api.rechazarReporte(id, RejectRequest(motivo)).requireBody()

    suspend fun tecnicos(): List<TechnicianResponse> = api.listarTecnicos().requireBody()

    suspend fun asignar(
        id: Long,
        tecnicoId: Long,
        prioridad: String,
        observacion: String?
    ): ReportResponse = api.asignarReporte(
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
