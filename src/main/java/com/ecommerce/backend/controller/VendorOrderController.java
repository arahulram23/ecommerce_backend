package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.OrderItemResponse;
import com.ecommerce.backend.service.CurrentUserService;
import com.ecommerce.backend.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Lets a vendor see the line items (across all customer orders) that belong to their store. */
@RestController
@RequestMapping("/api/vendor/orders")
public class VendorOrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public VendorOrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderItemResponse>> myOrderItems(Pageable pageable) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(orderService.getVendorOrderItems(userId, pageable));
    }
}
