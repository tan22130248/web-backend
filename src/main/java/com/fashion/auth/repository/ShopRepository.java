package com.fashion.auth.repository;

import com.fashion.auth.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, String> {
    List<Shop> findTop6ByVerifiedAtNotNullOrderByVerifiedAtDesc();
    Optional<Shop> findByUserId(String userId);
    boolean existsByUserId(String userId);

    @Query("SELECT s FROM Shop s WHERE " +
           "LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(s.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY s.totalPoints DESC, s.avgRating DESC, s.createdAt DESC")
    List<Shop> searchShops(@Param("keyword") String keyword);
}