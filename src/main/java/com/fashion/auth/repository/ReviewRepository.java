package com.fashion.auth.repository;

import com.fashion.auth.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, String>, JpaSpecificationExecutor<Review> {
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    Page<Review> findByProductIdAndRatingOrderByCreatedAtDesc(String productId, int rating, Pageable pageable);

    List<Review> findByUserId(String userId);

    boolean existsByUserIdAndOrderId(String userId, String orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.shop.id = :shopId")
    Double getAverageRatingByShop(@Param("shopId") String shopId);

    long countByShopId(String shopId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.shop.id = :shopId AND EXISTS (SELECT rr FROM ReviewReply rr WHERE rr.review.id = r.id)")
    long countRepliedReviewsByShop(@Param("shopId") String shopId);

    boolean existsByUserIdAndProductId(String userId, String productId);

    long countByProductId(String productId);

    long countByProductIdAndRating(String productId, int rating);
}
