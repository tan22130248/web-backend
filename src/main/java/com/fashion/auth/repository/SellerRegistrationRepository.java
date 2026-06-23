package com.fashion.auth.repository;

import com.fashion.auth.model.SellerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, String> {
    Optional<SellerRegistration> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
