package com.ecommerce.backend.dto;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Long priceCents,
        Integer stock,
        String imageUrl,
        String vendorName,
        String categoryName
) {}
