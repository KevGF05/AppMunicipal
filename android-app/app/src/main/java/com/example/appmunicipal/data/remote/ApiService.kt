package com.example.appmunicipal.data.remote

import com.example.appmunicipal.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("api/public/reportes")
    suspend fun crearReporte(
        @Part("tipo") tipo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("direccion") direccion: RequestBody,
        @Part("latitud") latitud: RequestBody,
        @Part("longitud") longitud: RequestBody,
        @Part foto: MultipartBody.Part
    ): Response<CreateReportResponse>

    @GET("api/public/reportes/{codigo}")
    suspend fun consultarReporte(
        @Path("codigo") codigo: String,
        @Query("token") token: String
    ): Response<TrackingResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/admin/reportes")
    suspend fun listarReportesAdmin(
        @Query("estado") estado: String? = null
    ): Response<List<ReportResponse>>

    @GET("api/admin/reportes/{id}")
    suspend fun obtenerReporteAdmin(@Path("id") id: Long): Response<ReportResponse>

    @PATCH("api/admin/reportes/{id}/aprobar")
    suspend fun aprobarReporte(@Path("id") id: Long): Response<ReportResponse>

    @PATCH("api/admin/reportes/{id}/rechazar")
    suspend fun rechazarReporte(
        @Path("id") id: Long,
        @Body request: RejectRequest
    ): Response<ReportResponse>

    @PATCH("api/admin/reportes/{id}/asignar")
    suspend fun asignarReporte(
        @Path("id") id: Long,
        @Body request: AssignRequest
    ): Response<ReportResponse>

    @GET("api/admin/tecnicos")
    suspend fun listarTecnicos(): Response<List<TechnicianResponse>>

    @GET("api/tecnico/reportes")
    suspend fun listarReportesTecnico(): Response<List<ReportResponse>>

    @GET("api/tecnico/reportes/{id}")
    suspend fun obtenerReporteTecnico(@Path("id") id: Long): Response<ReportResponse>

    @PATCH("api/tecnico/reportes/{id}/iniciar")
    suspend fun iniciarReporte(@Path("id") id: Long): Response<ReportResponse>

    @Multipart
    @POST("api/tecnico/reportes/{id}/cerrar")
    suspend fun cerrarReporte(
        @Path("id") id: Long,
        @Part("comentario") comentario: RequestBody,
        @Part fotoFinal: MultipartBody.Part
    ): Response<ReportResponse>
}
