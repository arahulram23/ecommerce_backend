package com.ecommerce.backend.dto;

import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items
) {}
