package com.municipal.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
    Long id,
    String code,
    String type,
    String description,
    String address,
    double latitude,
    double longitude,
    String initialPhotoUrl,
    String finalPhotoUrl,
    String status,
    String priority,
    Long assignedTechnicianId,
    String assignedTechnicianName,
    String closureComment,
    String rejectionReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<HistoryResponse> history
) {}
