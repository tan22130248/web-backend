package com.fashion.auth.repository;

import com.fashion.auth.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);
    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    Page<Review> findByProductIdAndRatingOrderByCreatedAtDesc(String productId, int rating, Pageable pageable);
    List<Review> findByUserId(String userId);
    boolean existsByUserIdAndOrderId(String userId, String orderId);
    boolean existsByUserIdAndProductId(String userId, String productId);
    long countByProductId(String productId);
    long countByProductIdAndRating(String productId, int rating);
}
