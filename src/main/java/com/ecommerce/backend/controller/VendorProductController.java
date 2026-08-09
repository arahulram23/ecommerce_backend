package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.ProductRequest;
import com.ecommerce.backend.dto.ProductResponse;
import com.ecommerce.backend.service.CurrentUserService;
import com.ecommerce.backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Product management for the authenticated vendor's own catalog. */
@RestController
@RequestMapping("/api/vendor/products")
public class VendorProductController {

    private final ProductService productService;
    private final CurrentUserService currentUserService;

    public VendorProductController(ProductService productService, CurrentUserService currentUserService) {
        this.productService = productService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(productService.createForVendor(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(productService.updateForVendor(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUser().getId();
        productService.deleteForVendor(userId, id);
        return ResponseEntity.noContent().build();
    }
}
