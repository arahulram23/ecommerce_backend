package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUserId(Long userId);
    List<Vendor> findByApprovedFalse();
}
