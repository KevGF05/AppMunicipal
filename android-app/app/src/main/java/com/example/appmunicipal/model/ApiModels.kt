package com.example.appmunicipal.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val name: String,
    val role: String,
    val passwordChangeRequired: Boolean
)

data class PasswordChangeRequest(
    val newPassword: String,
    val confirmPassword: String
)

data class MessageResponse(val message: String)

data class CreateReportResponse(
    val id: Long,
    val code: String,
    val status: String,
    val message: String
)

data class TrackingResponse(
    val code: String,
    val status: String,
    val updatedAt: String?,
    val message: String,
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
    val firstName: String?,
    val lastName: String?,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val active: Boolean,
    val passwordChangeRequired: Boolean
)

data class NotificationResponse(
    val id: Long,
    val title: String,
    val message: String,
    val reportId: Long?,
    val read: Boolean,
    val createdAt: String?
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

data class TechnicianCreateRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String
)

data class TechnicianUpdateRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String
)

data class TechnicianStatusRequest(
    val active: Boolean
)


data class ReportTypeResponse(
    val id: Long,
    val name: String,
    val active: Boolean
)

data class ReportTypeCreateRequest(
    val name: String
)
