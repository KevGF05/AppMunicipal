package com.municipal.backend.dto;

public record CreateReportResponse(
    Long id,
    String code,
    String trackingToken,
    String status,
    String message
) {}
