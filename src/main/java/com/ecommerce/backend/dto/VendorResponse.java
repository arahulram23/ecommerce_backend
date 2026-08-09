package com.ecommerce.backend.dto;

import java.time.LocalDateTime;

public record VendorResponse(
        Long id,
        String storeName,
        String description,
        boolean approved,
        LocalDateTime createdAt,
        String ownerEmail,
        String ownerFullName
) {}
