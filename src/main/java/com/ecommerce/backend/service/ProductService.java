package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ProductRequest;
import com.ecommerce.backend.dto.ProductResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.ApiException;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.VendorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, VendorRepository vendorRepository,
                           CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listActive(Long categoryId, String search, Pageable pageable) {
        Page<Product> page;
        if (search != null && !search.isBlank()) {
            page = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(search, pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
        } else {
            page = productRepository.findByActiveTrue(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        return toResponse(p);
    }

    @Transactional
    public ProductResponse createForVendor(Long userId, ProductRequest req) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No vendor profile for this account"));
        if (!vendor.isApproved()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vendor account is pending admin approval");
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));
        }

        Product product = Product.builder()
                .vendor(vendor)
                .category(category)
                .name(req.name())
                .description(req.description())
                .priceCents(req.priceCents())
                .stock(req.stock())
                .imageUrl(req.imageUrl())
                .active(true)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateForVendor(Long userId, Long productId, ProductRequest req) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No vendor profile for this account"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.getVendor().getId().equals(vendor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this product");
        }

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPriceCents(req.priceCents());
        product.setStock(req.stock());
        product.setImageUrl(req.imageUrl());

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));
            product.setCategory(category);
        }

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteForVendor(Long userId, Long productId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No vendor profile for this account"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!product.getVendor().getId().equals(vendor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this product");
        }
        product.setActive(false); // soft delete keeps order history intact
        productRepository.save(product);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPriceCents(),
                p.getStock(),
                p.getImageUrl(),
                p.getVendor().getStoreName(),
                p.getCategory() != null ? p.getCategory().getName() : null
        );
    }
}
