package com.fashion.auth.service;

import com.fashion.auth.dto.ReviewDto.*;
import com.fashion.auth.model.Order;
import com.fashion.auth.model.Product;
import com.fashion.auth.model.ProductComment;
import com.fashion.auth.model.Review;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewCommentService {

    private final ReviewRepository reviewRepository;
    private final ProductCommentRepository productCommentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(String productId, String email) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        long star5 = reviewRepository.countByProductIdAndRating(productId, 5);
        long star4 = reviewRepository.countByProductIdAndRating(productId, 4);
        long star3 = reviewRepository.countByProductIdAndRating(productId, 3);
        long star2 = reviewRepository.countByProductIdAndRating(productId, 2);
        long star1 = reviewRepository.countByProductIdAndRating(productId, 1);

        long totalReviews = star5 + star4 + star3 + star2 + star1;
        double averageRating = totalReviews == 0 ? 0.0 : (double) (star5 * 5 + star4 * 4 + star3 * 3 + star2 * 2 + star1) / totalReviews;

        Map<Integer, Long> distribution = new HashMap<>();
        distribution.put(5, star5);
        distribution.put(4, star4);
        distribution.put(3, star3);
        distribution.put(2, star2);
        distribution.put(1, star1);

        boolean hasPurchased = false;
        boolean hasRated = false;

        if (email != null && !email.trim().isEmpty()) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                hasPurchased = orderItemRepository.existsByProductIdAndOrderBuyerIdAndOrderStatus(
                        productId, user.getId(), Order.OrderStatus.delivered
                );
                hasRated = reviewRepository.existsByUserIdAndProductId(user.getId(), productId);
            }
        }

        return ReviewSummaryResponse.builder()
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .ratingDistribution(distribution)
                .hasPurchased(hasPurchased)
                .hasRated(hasRated)
                .build();
    }

    @Transactional
    public ReviewResponse createReview(String email, String productId, int rating, String comment) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        boolean hasPurchased = orderItemRepository.existsByProductIdAndOrderBuyerIdAndOrderStatus(
                productId, user.getId(), Order.OrderStatus.delivered
        );
        if (!hasPurchased) {
            throw new RuntimeException("Chỉ những khách hàng đã mua sản phẩm này mới được phép đánh giá.");
        }

        boolean hasRated = reviewRepository.existsByUserIdAndProductId(user.getId(), productId);
        if (hasRated) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi.");
        }

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }

        Review review = Review.builder()
                .user(user)
                .buyer(user)
                .product(product)
                .shop(product.getShop())
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        return ReviewResponse.builder()
                .id(saved.getId())
                .rating(saved.getRating())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .userId(user.getId())
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(String productId, Integer rating, Pageable pageable) {
        Page<Review> reviewsPage;
        if (rating != null) {
            reviewsPage = reviewRepository.findByProductIdAndRatingOrderByCreatedAtDesc(productId, rating, pageable);
        } else {
            reviewsPage = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }

        return reviewsPage.map(review -> ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatar(review.getUser().getAvatarUrl())
                .build());
    }

    @Transactional
    public CommentResponse createComment(String email, String productId, String content) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống.");
        }

        boolean isBuyer = orderItemRepository.existsByProductIdAndOrderBuyerIdAndOrderStatus(
                productId, user.getId(), Order.OrderStatus.delivered
        );

        ProductComment comment = ProductComment.builder()
                .user(user)
                .product(product)
                .content(content)
                .isBuyer(isBuyer)
                .createdAt(LocalDateTime.now())
                .build();

        ProductComment saved = productCommentRepository.save(comment);

        return CommentResponse.builder()
                .id(saved.getId())
                .content(saved.getContent())
                .isBuyer(saved.isBuyer())
                .createdAt(saved.getCreatedAt())
                .userId(user.getId())
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String productId, boolean buyerOnly, Pageable pageable) {
        Page<ProductComment> commentsPage;
        if (buyerOnly) {
            commentsPage = productCommentRepository.findByProductIdAndIsBuyerTrueOrderByCreatedAtDesc(productId, pageable);
        } else {
            commentsPage = productCommentRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }

        return commentsPage.map(comment -> CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .isBuyer(comment.isBuyer())
                .createdAt(comment.getCreatedAt())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFullName())
                .userAvatar(comment.getUser().getAvatarUrl())
                .build());
    }
}
