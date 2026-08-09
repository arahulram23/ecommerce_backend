package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CheckoutRequest;
import com.ecommerce.backend.dto.OrderResponse;
import com.ecommerce.backend.service.CurrentUserService;
import com.ecommerce.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest req) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(orderService.checkout(userId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(orderService.getOwnedOrder(userId, id));
    }
}
