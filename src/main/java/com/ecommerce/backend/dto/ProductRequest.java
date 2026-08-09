package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero Long priceCents,
        @NotNull @PositiveOrZero Integer stock,
        Long categoryId,
        String imageUrl
) {}
