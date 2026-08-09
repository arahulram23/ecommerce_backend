package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.CurrentUserService;
import com.ecommerce.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService, CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/orders/{orderId}/intent")
    public ResponseEntity<Map<String, String>> createIntent(@PathVariable Long orderId) {
        Long userId = currentUserService.getCurrentUser().getId();
        String clientSecret = paymentService.createPaymentIntent(userId, orderId);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    /**
     * Stripe webhook receiver. Must read the RAW request body (not a parsed DTO) because
     * signature verification is computed over the exact bytes Stripe sent.
     * This path is permitAll'd in SecurityConfig -- Stripe can't send a JWT, so it's
     * secured instead by verifying the Stripe-Signature header inside processWebhookEvent.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(HttpServletRequest request,
                                               @RequestHeader("Stripe-Signature") String signature) throws IOException {
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        paymentService.processWebhookEvent(payload, signature);
        return ResponseEntity.ok().build();
    }
}
