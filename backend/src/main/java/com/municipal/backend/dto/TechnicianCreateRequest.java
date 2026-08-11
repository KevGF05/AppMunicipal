package com.municipal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TechnicianCreateRequest(
    @NotBlank @Size(min = 2, max = 60) String firstName,
    @NotBlank @Size(min = 2, max = 80) String lastName,
    @NotBlank @Size(min = 8, max = 30) String phoneNumber
) {}
