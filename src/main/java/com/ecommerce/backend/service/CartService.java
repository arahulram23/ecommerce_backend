package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CartItemRequest;
import com.ecommerce.backend.dto.CartItemResponse;
import com.ecommerce.backend.dto.CartResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.exception.ApiException;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return toResponse(loadCart(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest req) {
        Cart cart = loadCart(userId);
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        // Check the combined quantity (existing line + this add), not just the requested delta --
        // otherwise a second small add can push an existing line past available stock unnoticed.
        int newQuantity = (existing != null ? existing.getQuantity() : 0) + req.quantity();
        if (product.getStock() < newQuantity) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough stock for " + product.getName());
        }

        if (existing != null) {
            existing.setQuantity(newQuantity);
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(req.quantity())
                    .build());
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = loadCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not in cart"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (item.getProduct().getStock() < quantity) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough stock for " + item.getProduct().getName());
            }
            item.setQuantity(quantity);
        }
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = loadCart(userId);
        cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clear(Long userId) {
        Cart cart = loadCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart loadCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cart not found"));
    }

    private CartResponse toResponse(Cart cart) {
        return new CartResponse(cart.getId(), cart.getItems().stream().map(this::toItemResponse).toList());
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        return new CartItemResponse(item.getId(), product.getId(), product.getName(),
                product.getPriceCents(), item.getQuantity());
    }
}
