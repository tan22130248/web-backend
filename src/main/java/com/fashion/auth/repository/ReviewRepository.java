package com.fashion.auth.repository;

import com.fashion.auth.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);
    List<Review> findByUserId(String userId);
    boolean existsByUserIdAndOrderId(String userId, String orderId);
}
