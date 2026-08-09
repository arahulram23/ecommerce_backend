package com.ecommerce.backend.dto;

import com.ecommerce.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank String fullName,
        @NotBlank String role // "CUSTOMER" or "VENDOR" -- ADMIN accounts are never self-registered
) {
    public Role roleEnum() {
        Role r = Role.valueOf(role.toUpperCase());
        if (r == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot self-register as ADMIN");
        }
        return r;
    }
}
