package com.municipal.backend.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String title,
    String message,
    Long reportId,
    boolean read,
    LocalDateTime createdAt
) {}
