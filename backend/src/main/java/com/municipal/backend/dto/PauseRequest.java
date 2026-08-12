package com.municipal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PauseRequest(
    @NotBlank(message = "El motivo de la pausa es obligatorio.")
    @Size(min = 5, max = 500, message = "El motivo debe contener entre 5 y 500 caracteres.")
    String reason
) {}
