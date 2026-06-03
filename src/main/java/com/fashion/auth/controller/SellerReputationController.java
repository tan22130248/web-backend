package com.fashion.auth.controller;

import com.fashion.auth.model.User;
import com.fashion.auth.model.Order;
import com.fashion.auth.repository.*;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller/reputation")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SellerReputationController {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final ShopPointRepository shopPointRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public SellerReputationController(ReviewRepository reviewRepository, OrderRepository orderRepository, ShopRepository shopRepository, ShopPointRepository shopPointRepository, JwtUtils jwtUtils, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
        this.shopPointRepository = shopPointRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    private String getShopId(String token) {
        String jwt = token.replace("Bearer ", "");
        String email = jwtUtils.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return shopRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Shop not found")).getId();
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestHeader("Authorization") String token) {
        try {
            String shopId = getShopId(token);
            Double averageRating = reviewRepository.getAverageRatingByShop(shopId);
            if (averageRating == null) averageRating = 0.0;

            long totalReviews = reviewRepository.countByShopId(shopId);
            long repliedReviews = reviewRepository.countRepliedReviewsByShop(shopId);
            double responseRate = totalReviews > 0 ? (double) repliedReviews / totalReviews : 1.0;

            long totalOrders = orderRepository.countByShopId(shopId);
            long cancelledOrders = orderRepository.countByShopIdAndStatus(shopId, Order.OrderStatus.cancelled);
            double cancellationRate = totalOrders > 0 ? (double) cancelledOrders / totalOrders : 0.0;

            Integer currentPoints = shopPointRepository.sumPointsByShopId(shopId);
            if (currentPoints == null) currentPoints = 100; // Base default if no history

            return ResponseEntity.ok(Map.of(
                    "averageRating", averageRating,
                    "totalReviews", totalReviews,
                    "responseRate", responseRate,
                    "cancellationRate", cancellationRate,
                    "currentPoints", currentPoints
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
