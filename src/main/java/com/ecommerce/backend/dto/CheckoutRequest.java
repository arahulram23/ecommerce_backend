package com.ecommerce.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank String shippingAddress
) {}
