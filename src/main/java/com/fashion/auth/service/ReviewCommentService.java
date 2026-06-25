package com.fashion.auth.service;

import com.fashion.auth.dto.ReviewDto.*;
import com.fashion.auth.model.*;
import com.fashion.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewCommentService {

    private final ReviewRepository reviewRepository;
    private final ProductCommentRepository productCommentRepository;
    private final CommentReplyRepository commentReplyRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(String productId, String email) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Sản phẩm không tồn tại");
        }

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

        java.util.List<com.fashion.auth.model.OrderItem> items = orderItemRepository.findByProductIdAndOrderBuyerIdAndOrderStatus(
                productId, user.getId(), Order.OrderStatus.delivered
        );
        if (items.isEmpty()) {
            throw new RuntimeException("Chỉ những khách hàng đã mua sản phẩm này mới được phép đánh giá.");
        }
        Order order = items.get(0).getOrder();

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
                .order(order)
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
        return getReviews(productId, rating, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(String productId, Integer rating, Pageable pageable, String currentUserEmail) {
        Page<Review> reviewsPage;
        if (rating != null) {
            reviewsPage = reviewRepository.findByProductIdAndRatingOrderByCreatedAtDesc(productId, rating, pageable);
        } else {
            reviewsPage = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }

        return reviewsPage.map(review -> buildReviewResponse(review, currentUserEmail, productId));
    }

    private ReviewResponse buildReviewResponse(Review review, String currentUserEmail, String productId) {
        List<ReviewReply> replies = reviewReplyRepository.findByReviewIdOrderByCreatedAtAsc(review.getId());
        List<ReviewReplyResponse> replyResponses = replies.stream()
                .map(this::buildReviewReplyResponse)
                .collect(Collectors.toList());

        boolean canReply = false;
        if (currentUserEmail != null && !currentUserEmail.trim().isEmpty()) {
            User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (currentUser != null && currentUser.getRole().equals(User.Role.seller)) {
                Shop shop = shopRepository.findByUserId(currentUser.getId()).orElse(null);
                if (shop != null && review.getProduct().getShop().getId().equals(shop.getId())) {
                    canReply = !reviewReplyRepository.existsByReviewIdAndShopId(review.getId(), shop.getId());
                }
            }
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatar(review.getUser().getAvatarUrl())
                .replies(replyResponses)
                .canReply(canReply)
                .build();
    }

    private ReviewReplyResponse buildReviewReplyResponse(ReviewReply reply) {
        return ReviewReplyResponse.builder()
                .id(reply.getId())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .shopId(reply.getShop().getId())
                .shopName(reply.getShop().getShopName())
                .shopAvatar(reply.getShop().getAvatarUrl())
                .userId(reply.getUser().getId())
                .userName(reply.getUser().getFullName())
                .userAvatar(reply.getUser().getAvatarUrl())
                .build();
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

        return buildCommentResponse(saved, email);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String productId, boolean buyerOnly, Pageable pageable) {
        return getComments(productId, buyerOnly, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String productId, boolean buyerOnly, Pageable pageable, String currentUserEmail) {
        Page<ProductComment> commentsPage;
        if (buyerOnly) {
            commentsPage = productCommentRepository.findByProductIdAndIsBuyerTrueOrderByCreatedAtDesc(productId, pageable);
        } else {
            commentsPage = productCommentRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        }

        return commentsPage.map(comment -> buildCommentResponse(comment, currentUserEmail));
    }

 
    @Transactional
    public CommentReplyResponse createCommentReply(String email, String commentId, String content) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!user.getRole().equals(User.Role.seller)) {
            throw new RuntimeException("Chỉ shop owner mới có thể trả lời bình luận");
        }

        ProductComment comment = productCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        if (!comment.getProduct().getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Bạn chỉ có thể trả lời bình luận cho sản phẩm của shop mình");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung trả lời không được để trống");
        }

        boolean alreadyReplied = commentReplyRepository.existsByCommentIdAndShopId(commentId, shop.getId());
        if (alreadyReplied) {
            throw new RuntimeException("Shop đã trả lời bình luận này rồi");
        }

        CommentReply reply = CommentReply.builder()
                .comment(comment)
                .shop(shop)
                .user(user)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CommentReply saved = commentReplyRepository.save(reply);

        return buildCommentReplyResponse(saved);
    }

 
    @Transactional
    public CommentReplyResponse updateCommentReply(String email, String replyId, String content) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        CommentReply reply = commentReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply không tồn tại"));

        if (!reply.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn chỉ có thể sửa reply của mình");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung trả lời không được để trống");
        }

        reply.setContent(content);
        reply.setUpdatedAt(LocalDateTime.now());

        CommentReply updated = commentReplyRepository.save(reply);
        return buildCommentReplyResponse(updated);
    }

    @Transactional
    public void deleteCommentReply(String email, String replyId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        CommentReply reply = commentReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply không tồn tại"));

        if (!reply.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn chỉ có thể xóa reply của mình");
        }

        commentReplyRepository.delete(reply);
    }

 
    @Transactional(readOnly = true)
    public List<CommentReplyResponse> getCommentReplies(String commentId) {
        List<CommentReply> replies = commentReplyRepository.findByCommentIdOrderByCreatedAtAsc(commentId);
        return replies.stream()
                .map(this::buildCommentReplyResponse)
                .collect(Collectors.toList());
    }

 
    @Transactional
    public ReviewReplyResponse createReviewReply(String email, String reviewId, String content) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!user.getRole().equals(User.Role.seller)) {
            throw new RuntimeException("Chỉ shop owner mới có thể trả lời đánh giá");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));

        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Shop không tồn tại"));

        if (!review.getProduct().getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Bạn chỉ có thể trả lời đánh giá cho sản phẩm của shop mình");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung trả lời không được để trống");
        }

        boolean alreadyReplied = reviewReplyRepository.existsByReviewIdAndShopId(reviewId, shop.getId());
        if (alreadyReplied) {
            throw new RuntimeException("Shop đã trả lời đánh giá này rồi");
        }

        ReviewReply reply = ReviewReply.builder()
                .review(review)
                .shop(shop)
                .user(user)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReviewReply saved = reviewReplyRepository.save(reply);

        return buildReviewReplyResponse(saved);
    }

    @Transactional
    public ReviewReplyResponse updateReviewReply(String email, String replyId, String content) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply không tồn tại"));

        if (!reply.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn chỉ có thể sửa reply của mình");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Nội dung trả lời không được để trống");
        }

        reply.setContent(content);
        reply.setUpdatedAt(LocalDateTime.now());

        ReviewReply updated = reviewReplyRepository.save(reply);
        return buildReviewReplyResponse(updated);
    }

    @Transactional
    public void deleteReviewReply(String email, String replyId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply không tồn tại"));

        if (!reply.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn chỉ có thể xóa reply của mình");
        }

        reviewReplyRepository.delete(reply);
    }

   
    @Transactional(readOnly = true)
    public List<ReviewReplyResponse> getReviewReplies(String reviewId) {
        List<ReviewReply> replies = reviewReplyRepository.findByReviewIdOrderByCreatedAtAsc(reviewId);
        return replies.stream()
                .map(this::buildReviewReplyResponse)
                .collect(Collectors.toList());
    }

   
    @Transactional
    public void deleteComment(String email, String productId, String commentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        ProductComment comment = productCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Sản phẩm không tồn tại");
        }

        boolean canDelete = false;

        if (user.getRole().equals(User.Role.admin)) {
            canDelete = true;
        }
        else if (user.getRole().equals(User.Role.seller)) {
            Shop shop = shopRepository.findByUserId(user.getId()).orElse(null);
            if (shop != null && comment.getProduct().getShop().getId().equals(shop.getId())) {
                canDelete = true;
            }
        }
        else if (comment.getUser().getId().equals(user.getId())) {
            canDelete = true;
        }

        if (!canDelete) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        productCommentRepository.delete(comment);
    }

    private CommentResponse buildCommentResponse(ProductComment comment, String currentUserEmail) {
        List<CommentReply> replies = commentReplyRepository.findByCommentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentReplyResponse> replyResponses = replies.stream()
                .map(this::buildCommentReplyResponse)
                .collect(Collectors.toList());

        boolean canReply = false;
        boolean canDelete = false;
        
        if (currentUserEmail != null && !currentUserEmail.trim().isEmpty()) {
            User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (currentUser != null) {
                if (currentUser.getRole().equals(User.Role.seller)) {
                    Shop shop = shopRepository.findByUserId(currentUser.getId()).orElse(null);
                    if (shop != null && comment.getProduct().getShop().getId().equals(shop.getId())) {
                        canReply = !commentReplyRepository.existsByCommentIdAndShopId(comment.getId(), shop.getId());
                        canDelete = true;
                    }
                }
                
          
                if (currentUser.getRole().equals(User.Role.admin)) {
                    canDelete = true;
                }
                if (comment.getUser().getId().equals(currentUser.getId())) {
                    canDelete = true;
                }
            }
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .isBuyer(comment.isBuyer())
                .createdAt(comment.getCreatedAt())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFullName())
                .userAvatar(comment.getUser().getAvatarUrl())
                .replies(replyResponses)
                .canReply(canReply)
                .canDelete(canDelete)
                .build();
    }

    private CommentReplyResponse buildCommentReplyResponse(CommentReply reply) {
        return CommentReplyResponse.builder()
                .id(reply.getId())
                .content(reply.getContent())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .shopId(reply.getShop().getId())
                .shopName(reply.getShop().getShopName())
                .shopAvatar(reply.getShop().getAvatarUrl())
                .userId(reply.getUser().getId())
                .userName(reply.getUser().getFullName())
                .userAvatar(reply.getUser().getAvatarUrl())
                .build();
    }
}
