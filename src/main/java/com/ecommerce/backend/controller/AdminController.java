package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.VendorResponse;
import com.ecommerce.backend.service.VendorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only endpoints. Route is protected by SecurityConfig (hasRole("ADMIN")). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final VendorService vendorService;

    public AdminController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping("/vendors/pending")
    public ResponseEntity<List<VendorResponse>> pendingVendors() {
        return ResponseEntity.ok(vendorService.listPending());
    }

    @PostMapping("/vendors/{id}/approve")
    public ResponseEntity<VendorResponse> approveVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.approve(id));
    }
}
