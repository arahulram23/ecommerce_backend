package com.ecommerce.backend.dto;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        Long vendorId,
        String vendorStoreName,
        Integer quantity,
        Long priceCentsAtPurchase
) {}