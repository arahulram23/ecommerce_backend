package com.ecommerce.backend.dto;

public record AuthResponse(
        String token,
        String email,
        String role,
        String fullName
) {}
