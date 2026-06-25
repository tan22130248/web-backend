package com.fashion.auth.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReviewDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewResponse {
        private String id;
        private int rating;
        private String comment;
        private LocalDateTime createdAt;
        private String userId;
        private String userName;
        private String userAvatar;
        private List<ReviewReplyResponse> replies;
        private boolean canReply; // Can current user (shop owner) reply to this review
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewReplyResponse {
        private String id;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String shopId;
        private String shopName;
        private String shopAvatar;
        private String userId;
        private String userName;
        private String userAvatar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentResponse {
        private String id;
        private String content;
        private boolean isBuyer;
        private LocalDateTime createdAt;
        private String userId;
        private String userName;
        private String userAvatar;
        private List<CommentReplyResponse> replies;
        private boolean canReply; // Can current user (shop owner) reply to this comment
        private boolean canDelete; // Can current user delete this comment
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentReplyResponse {
        private String id;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String shopId;
        private String shopName;
        private String shopAvatar;
        private String userId;
        private String userName;
        private String userAvatar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        private int rating;
        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentRequest {
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentReplyRequest {
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewSummaryResponse {
        private double averageRating;
        private long totalReviews;
        private Map<Integer, Long> ratingDistribution;
        private boolean hasPurchased;
        private boolean hasRated;
    }
}
