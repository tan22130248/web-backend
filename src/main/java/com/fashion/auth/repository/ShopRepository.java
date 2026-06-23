package com.fashion.auth.repository;

import com.fashion.auth.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, String> {
    List<Shop> findTop6ByVerifiedAtNotNullOrderByVerifiedAtDesc();
    Optional<Shop> findByUserId(String userId);
    boolean existsByUserId(String userId);
}

