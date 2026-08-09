package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CheckoutRequest;
import com.ecommerce.backend.dto.OrderItemResponse;
import com.ecommerce.backend.dto.OrderResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.ApiException;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(CartRepository cartRepository, OrderRepository orderRepository,
                         ProductRepository productRepository, UserRepository userRepository,
                         VendorRepository vendorRepository, OrderItemRepository orderItemRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Creates a PENDING order from the user's current cart, decrements stock, and empties the cart.
     * Payment is handled separately (see PaymentService) -- this only reserves the order + stock.
     */
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest req) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        long total = 0L;
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(req.shippingAddress())
                .totalCents(0L) // set below
                .build();

        // Process items in a stable product-id order so concurrent multi-item checkouts always
        // acquire row locks in the same order and can't deadlock against each other.
        var sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();

        for (CartItem item : sortedItems) {
            Product product = item.getProduct();

            // Atomic, race-safe stock check: an in-memory "stock < quantity" check followed by a
            // separate save can't prevent two concurrent checkouts from both passing the check
            // before either commits. The conditional UPDATE below is the actual source of truth.
            int updated = productRepository.decrementStock(product.getId(), item.getQuantity());
            if (updated == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough stock for " + product.getName());
            }

            long lineTotal = product.getPriceCents() * item.getQuantity();
            total += lineTotal;

            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .vendor(product.getVendor())
                    .quantity(item.getQuantity())
                    .priceCentsAtPurchase(product.getPriceCents())
                    .build());
        }

        order.setTotalCents(total);
        Order saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOwnedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This order does not belong to you");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderItemResponse> getVendorOrderItems(Long userId, Pageable pageable) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No vendor profile for this account"));
        return orderItemRepository.findByVendorId(vendor.getId(), pageable).map(this::toItemResponse);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalCents(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getVendor().getId(),
                item.getVendor().getStoreName(),
                item.getQuantity(),
                item.getPriceCentsAtPurchase()
        );
    }
}
