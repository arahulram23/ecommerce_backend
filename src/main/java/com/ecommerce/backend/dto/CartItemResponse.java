package com.ecommerce.backend.dto;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        Long priceCents,
        Integer quantity
) {}
