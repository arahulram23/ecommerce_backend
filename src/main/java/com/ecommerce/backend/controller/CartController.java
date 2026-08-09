package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CartItemRequest;
import com.ecommerce.backend.dto.CartResponse;
import com.ecommerce.backend.service.CartService;
import com.ecommerce.backend.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartItemRequest req) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(cartService.addItem(userId, req));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long productId, @RequestParam int quantity) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }
}
