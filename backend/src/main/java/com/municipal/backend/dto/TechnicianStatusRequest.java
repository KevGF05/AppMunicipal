package com.municipal.backend.dto;

import jakarta.validation.constraints.NotNull;

public record TechnicianStatusRequest(
    @NotNull Boolean active
) {}
