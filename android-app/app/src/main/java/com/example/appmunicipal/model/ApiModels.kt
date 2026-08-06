package com.example.appmunicipal.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val name: String,
    val role: String
)

data class CreateReportResponse(
    val id: Long,
    val code: String,
    val trackingToken: String,
    val status: String,
    val message: String
)

data class TrackingResponse(
    val code: String,
    val type: String,
    val description: String,
    val address: String,
    val status: String,
    val priority: String,
    val createdAt: String?,
    val updatedAt: String?,
    val rejectionReason: String?,
    val closureComment: String?
)

data class HistoryResponse(
    val previousStatus: String?,
    val newStatus: String,
    val actor: String,
    val comment: String?,
    val createdAt: String?
)

data class ReportResponse(
    val id: Long,
    val code: String,
    val type: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val initialPhotoUrl: String?,
    val finalPhotoUrl: String?,
    val status: String,
    val priority: String,
    val assignedTechnicianId: Long?,
    val assignedTechnicianName: String?,
    val closureComment: String?,
    val rejectionReason: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val history: List<HistoryResponse> = emptyList()
)

data class TechnicianResponse(
    val id: Long,
    val name: String,
    val email: String
)

data class RejectRequest(val reason: String)

data class AssignRequest(
    val technicianId: Long,
    val priority: String,
    val observation: String?
)

data class ErrorResponse(
    val status: Int?,
    val message: String?,
    val timestamp: String?
)
