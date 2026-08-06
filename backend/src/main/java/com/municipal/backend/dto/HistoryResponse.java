package com.municipal.backend.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
    String previousStatus,
    String newStatus,
    String actor,
    String comment,
    LocalDateTime createdAt
) {}
