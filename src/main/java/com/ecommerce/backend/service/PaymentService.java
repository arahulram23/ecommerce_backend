package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.ApiException;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * Wraps Stripe's PaymentIntent API. Configure app.stripe.secret-key (env STRIPE_SECRET_KEY) for real use;
 * with the placeholder test key, createPaymentIntent will throw -- that's expected until you add real keys.
 */
@Service
public class PaymentService {

    @Value("${app.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    void init() {
        com.stripe.Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public String createPaymentIntent(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This order does not belong to you");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order is not payable in its current state");
        }

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(order.getTotalCents())
                    .setCurrency("usd")
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Payment is one-to-one with Order (see UNIQUE(order_id) in the schema), so a retry
            // after a prior failed attempt must update that existing row rather than insert a
            // second one -- otherwise the retry fails with a unique constraint violation.
            Payment payment = paymentRepository.findByOrderId(order.getId())
                    .orElseGet(() -> Payment.builder().order(order).build());
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmountCents(order.getTotalCents());
            paymentRepository.save(payment);

            return intent.getClientSecret(); // frontend uses this with Stripe.js to confirm the card payment
        } catch (StripeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage());
        }
    }

    /**
     * Call this from a Stripe webhook handler (payment_intent.succeeded) to mark the order paid.
     * Wire up a dedicated /api/payments/webhook endpoint with signature verification before going live.
     */
    @Transactional
    public void markSucceeded(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Payment not found for intent " + stripePaymentIntentId));

        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    @Transactional
    public void markFailed(String stripePaymentIntentId) {
        paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            // Order stays PENDING so the customer can retry payment; stock was already decremented
            // at checkout time, so no stock adjustment needed here.
        });
    }

    /**
     * Verifies the Stripe-Signature header against the raw request body, then dispatches
     * to the right handler based on event type. Call this from the webhook controller.
     */
    public void processWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(stripeObject instanceof PaymentIntent intent)) {
            return; // not a PaymentIntent event -- ignore
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> markSucceeded(intent.getId());
            case "payment_intent.payment_failed" -> markFailed(intent.getId());
            default -> { /* other event types intentionally ignored */ }
        }
    }
}
