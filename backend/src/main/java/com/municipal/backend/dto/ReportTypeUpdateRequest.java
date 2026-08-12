package com.municipal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportTypeUpdateRequest(
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(min = 3, max = 80, message = "El nombre debe contener entre 3 y 80 caracteres.")
    String name
) {}
