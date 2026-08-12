package com.municipal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportTypeCreateRequest(
    @NotBlank(message = "Ingrese el nombre del tipo de reporte.")
    @Size(min = 3, max = 80, message = "El tipo de reporte debe contener entre 3 y 80 caracteres.")
    String name
) {}
