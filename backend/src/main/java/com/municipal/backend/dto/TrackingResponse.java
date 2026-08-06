package com.municipal.backend.dto;

import java.time.LocalDateTime;

public record TrackingResponse(
    String code,
    String type,
    String description,
    String address,
    String status,
    String priority,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String rejectionReason,
    String closureComment
) {}
