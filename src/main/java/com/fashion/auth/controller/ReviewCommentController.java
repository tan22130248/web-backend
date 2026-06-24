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
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> response = reviewCommentService.getReviews(productId, rating, pageable);
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
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentResponse> response = reviewCommentService.getComments(productId, buyerOnly, pageable);
            return ResponseEntity.ok(response);
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
