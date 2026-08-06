package com.municipal.backend.dto;

import com.municipal.backend.model.Priority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignRequest(
    @NotNull Long technicianId,
    @NotNull Priority priority,
    @Size(max = 500) String observation
) {}
