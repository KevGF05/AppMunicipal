package com.municipal.backend.dto;

public record TechnicianResponse(
    Long id,
    String firstName,
    String lastName,
    String name,
    String email,
    String phoneNumber,
    boolean active,
    boolean passwordChangeRequired
) {}
