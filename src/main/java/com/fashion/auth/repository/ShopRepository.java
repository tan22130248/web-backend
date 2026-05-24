package com.fashion.auth.repository;

import com.fashion.auth.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, String> {
    Optional<Shop> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
