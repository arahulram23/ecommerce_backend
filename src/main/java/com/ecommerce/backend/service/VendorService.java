package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.VendorResponse;
import com.ecommerce.backend.entity.Vendor;
import com.ecommerce.backend.exception.ApiException;
import com.ecommerce.backend.repository.VendorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> listPending() {
        return vendorRepository.findByApprovedFalse().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VendorResponse approve(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vendor not found"));
        vendor.setApproved(true);
        return toResponse(vendorRepository.save(vendor));
    }

    private VendorResponse toResponse(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getStoreName(),
                vendor.getDescription(),
                vendor.isApproved(),
                vendor.getCreatedAt(),
                vendor.getUser().getEmail(),
                vendor.getUser().getFullName()
        );
    }
}
