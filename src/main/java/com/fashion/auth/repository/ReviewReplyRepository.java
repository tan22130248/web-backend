package com.fashion.auth.repository;

import com.fashion.auth.model.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, String> {

   
    @Query("SELECT rr FROM ReviewReply rr WHERE rr.review.id = :reviewId AND rr.shop.id = :shopId")
    Optional<ReviewReply> findByReviewIdAndShopId(@Param("reviewId") String reviewId, @Param("shopId") String shopId);

    @Query("SELECT rr FROM ReviewReply rr WHERE rr.review.id = :reviewId ORDER BY rr.createdAt ASC")
    List<ReviewReply> findByReviewIdOrderByCreatedAtAsc(@Param("reviewId") String reviewId);

    @Query("SELECT rr FROM ReviewReply rr WHERE rr.shop.id = :shopId ORDER BY rr.createdAt DESC")
    List<ReviewReply> findByShopIdOrderByCreatedAtDesc(@Param("shopId") String shopId);


    @Query("SELECT COUNT(rr) FROM ReviewReply rr WHERE rr.review.id = :reviewId")
    long countByReviewId(@Param("reviewId") String reviewId);


    @Query("SELECT COUNT(rr) > 0 FROM ReviewReply rr WHERE rr.review.id = :reviewId AND rr.shop.id = :shopId")
    boolean existsByReviewIdAndShopId(@Param("reviewId") String reviewId, @Param("shopId") String shopId);
}