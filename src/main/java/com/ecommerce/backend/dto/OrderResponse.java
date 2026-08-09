package com.ecommerce.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        Long totalCents,
        String shippingAddress,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {}
