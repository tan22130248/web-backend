package com.fashion.auth.controller;

import com.fashion.auth.model.User;
import com.fashion.auth.repository.OrderRepository;
import com.fashion.auth.repository.ProductRepository;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SellerDashboardController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public SellerDashboardController(OrderRepository orderRepository, ProductRepository productRepository, ShopRepository shopRepository, JwtUtils jwtUtils, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
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
            BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(shopId);
            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

            long newOrders = orderRepository.countNewOrders(shopId);
            long productsOnline = productRepository.findByShopId(shopId).stream().filter(com.fashion.auth.model.Product::isActive).count();

            return ResponseEntity.ok(Map.of(
                    "totalRevenue", totalRevenue,
                    "newOrders", newOrders,
                    "productsOnline", productsOnline
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<?> getRevenueChart(@RequestHeader("Authorization") String token,
                                             @RequestParam(defaultValue = "7") int days) {
        try {
            String shopId = getShopId(token);
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            return ResponseEntity.ok(orderRepository.getRevenueChartData(shopId, startDate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/category-chart")
    public ResponseEntity<?> getCategoryChart(@RequestHeader("Authorization") String token) {
        try {
            String shopId = getShopId(token);
            return ResponseEntity.ok(orderRepository.getCategoryRevenueData(shopId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
