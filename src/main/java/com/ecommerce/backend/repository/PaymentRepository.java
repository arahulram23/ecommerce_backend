package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
