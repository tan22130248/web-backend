package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.ReviewDto.*;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.service.ReviewCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReviewCommentController {

    private final ReviewCommentService reviewCommentService;
    private final JwtUtils jwtUtils;

    @GetMapping("/reviews/summary")
    public ResponseEntity<?> getReviewSummary(
            @PathVariable String productId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String email = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                if (jwtUtils.validateToken(jwt)) {
                    email = jwtUtils.getEmailFromToken(jwt);
                }
            }
            return ResponseEntity.ok(reviewCommentService.getReviewSummary(productId, email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable String productId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReviewRequest request) {
        try {
            String email = getEmail(authHeader);
            ReviewResponse response = reviewCommentService.createReview(
                    email, productId, request.getRating(), request.getComment()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/reviews")
    public ResponseEntity<?> getReviews(
            @PathVariable String productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String email = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                if (jwtUtils.validateToken(jwt)) {
                    email = jwtUtils.getEmailFromToken(jwt);
                }
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> response = reviewCommentService.getReviews(productId, rating, pageable, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/comments")
    public ResponseEntity<?> createComment(
            @PathVariable String productId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CommentRequest request) {
        try {
            String email = getEmail(authHeader);
            CommentResponse response = reviewCommentService.createComment(
                    email, productId, request.getContent()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/comments")
    public ResponseEntity<?> getComments(
            @PathVariable String productId,
            @RequestParam(defaultValue = "false") boolean buyerOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String email = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                if (jwtUtils.validateToken(jwt)) {
                    email = jwtUtils.getEmailFromToken(jwt);
                }
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentResponse> response = reviewCommentService.getComments(productId, buyerOnly, pageable, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }


    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<?> replyToComment(
            @PathVariable String productId,
            @PathVariable String commentId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CommentReplyRequest request) {
        try {
            String email = getEmail(authHeader);
            CommentReplyResponse response = reviewCommentService.createCommentReply(
                    email, commentId, request.getContent()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }


    @PutMapping("/comments/{commentId}/reply/{replyId}")
    public ResponseEntity<?> updateCommentReply(
            @PathVariable String productId,
            @PathVariable String commentId,
            @PathVariable String replyId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CommentReplyRequest request) {
        try {
            String email = getEmail(authHeader);
            CommentReplyResponse response = reviewCommentService.updateCommentReply(
                    email, replyId, request.getContent()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}/reply/{replyId}")
    public ResponseEntity<?> deleteCommentReply(
            @PathVariable String productId,
            @PathVariable String commentId,
            @PathVariable String replyId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String email = getEmail(authHeader);
            reviewCommentService.deleteCommentReply(email, replyId);
            return ResponseEntity.ok(new MessageResponse("Đã xóa phản hồi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<?> getCommentReplies(
            @PathVariable String productId,
            @PathVariable String commentId) {
        try {
            List<CommentReplyResponse> replies = reviewCommentService.getCommentReplies(commentId);
            return ResponseEntity.ok(replies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }


    @PostMapping("/reviews/{reviewId}/reply")
    public ResponseEntity<?> replyToReview(
            @PathVariable String productId,
            @PathVariable String reviewId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CommentReplyRequest request) {
        try {
            String email = getEmail(authHeader);
            ReviewReplyResponse response = reviewCommentService.createReviewReply(
                    email, reviewId, request.getContent()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/reviews/{reviewId}/reply/{replyId}")
    public ResponseEntity<?> updateReviewReply(
            @PathVariable String productId,
            @PathVariable String reviewId,
            @PathVariable String replyId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CommentReplyRequest request) {
        try {
            String email = getEmail(authHeader);
            ReviewReplyResponse response = reviewCommentService.updateReviewReply(
                    email, replyId, request.getContent()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/reviews/{reviewId}/reply/{replyId}")
    public ResponseEntity<?> deleteReviewReply(
            @PathVariable String productId,
            @PathVariable String reviewId,
            @PathVariable String replyId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String email = getEmail(authHeader);
            reviewCommentService.deleteReviewReply(email, replyId);
            return ResponseEntity.ok(new MessageResponse("Đã xóa phản hồi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

  
    @GetMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<?> getReviewReplies(
            @PathVariable String productId,
            @PathVariable String reviewId) {
        try {
            List<ReviewReplyResponse> replies = reviewCommentService.getReviewReplies(reviewId);
            return ResponseEntity.ok(replies);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

 
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable String productId,
            @PathVariable String commentId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String email = getEmail(authHeader);
            reviewCommentService.deleteComment(email, productId, commentId);
            return ResponseEntity.ok(new MessageResponse("Đã xóa bình luận thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private String getEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ");
        }
        String jwt = authHeader.substring(7);
        if (!jwtUtils.validateToken(jwt)) {
            throw new RuntimeException("Token không hợp lệ");
        }
        String email = jwtUtils.getEmailFromToken(jwt);
        if (email == null) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return email;
    }
}
