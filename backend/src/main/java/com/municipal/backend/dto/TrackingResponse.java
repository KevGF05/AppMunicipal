package com.municipal.backend.dto;

import java.time.LocalDateTime;

public record TrackingResponse(
    String code,
    String status,
    LocalDateTime updatedAt,
    String message,
    String closureComment
) {}
