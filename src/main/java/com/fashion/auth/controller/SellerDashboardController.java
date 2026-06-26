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
        System.out.println("=== DEBUG getShopId ===");
        System.out.println("Email from token: " + email);
        
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("User found - ID: " + user.getId() + ", FullName: " + user.getFullName() + ", Role: " + user.getRole());
        
        String shopId = shopRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Shop not found")).getId();
        System.out.println("Shop found - Shop ID: " + shopId);
        System.out.println("======================");
        
        return shopId;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestHeader("Authorization") String token) {
        try {
            System.out.println("\n=== /api/seller/dashboard/summary CALLED ===");
            String shopId = getShopId(token);
            
            BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(shopId);
            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
            System.out.println("Total Revenue: " + totalRevenue);

            long newOrders = orderRepository.countNewOrders(shopId);
            System.out.println("New Orders (pending): " + newOrders);
            
            long productsOnline = productRepository.findByShopId(shopId).stream().filter(com.fashion.auth.model.Product::isActive).count();
            System.out.println("Products Online: " + productsOnline);
            System.out.println("===========================================\n");

            return ResponseEntity.ok(Map.of(
                    "totalRevenue", totalRevenue,
                    "newOrders", newOrders,
                    "productsOnline", productsOnline
            ));
        } catch (Exception e) {
            System.err.println("ERROR in /summary: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<?> getRevenueChart(@RequestHeader("Authorization") String token,
                                             @RequestParam(defaultValue = "7") int days) {
        try {
            System.out.println("\n=== /api/seller/dashboard/revenue-chart CALLED (days=" + days + ") ===");
            String shopId = getShopId(token);
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            System.out.println("Start Date: " + startDate);
            
            var result = orderRepository.getRevenueChartData(shopId, startDate);
            System.out.println("Revenue Chart Data Count: " + result.size());
            System.out.println("===================================================================\n");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("ERROR in /revenue-chart: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue-chart-by-month")
    public ResponseEntity<?> getRevenueChartByMonth(@RequestHeader("Authorization") String token,
                                                     @RequestParam int month,
                                                     @RequestParam int year) {
        try {
            System.out.println("\n=== /api/seller/dashboard/revenue-chart-by-month CALLED (month=" + month + ", year=" + year + ") ===");
            String shopId = getShopId(token);
            
            var result = orderRepository.getRevenueChartDataByMonth(shopId, month, year);
            System.out.println("Revenue Chart Data Count: " + result.size());
            System.out.println("===================================================================================\n");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("ERROR in /revenue-chart-by-month: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/revenue-chart-by-month-days")
    public ResponseEntity<?> getRevenueChartByMonthAndDays(@RequestHeader("Authorization") String token,
                                                            @RequestParam(required = false, defaultValue = "0") Integer month,
                                                            @RequestParam(required = false, defaultValue = "0") Integer year,
                                                            @RequestParam(required = false, defaultValue = "30") Integer days) {
        try {
            System.out.println("\n=== /api/seller/dashboard/revenue-chart-by-month-days CALLED (month=" + month + ", year=" + year + ", days=" + days + ") ===");
            String shopId = getShopId(token);
            
            // Use current month/year if not provided or if 0
            LocalDateTime now = LocalDateTime.now();
            int actualMonth = (month != null && month > 0) ? month : now.getMonthValue();
            int actualYear = (year != null && year > 0) ? year : now.getYear();
            int actualDays = (days != null && days > 0) ? days : 30;
            
            // Calculate start date: go back 'days' from today, but only within the selected month/year
            LocalDateTime startDate = now.minusDays(actualDays);
            
            // Filter to get data only for the selected month/year within the days range
            var result = orderRepository.getRevenueChartDataByMonthAndDays(shopId, actualMonth, actualYear, startDate);
            System.out.println("Revenue Chart Data Count: " + result.size());
            System.out.println("Start Date: " + startDate);
            System.out.println("============================================================================================\n");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("ERROR in /revenue-chart-by-month-days: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/category-chart")
    public ResponseEntity<?> getCategoryChart(@RequestHeader("Authorization") String token,
                                              @RequestParam(required = false, defaultValue = "0") Integer month,
                                              @RequestParam(required = false, defaultValue = "0") Integer year) {
        try {
            System.out.println("\n=== /api/seller/dashboard/category-chart CALLED (month=" + month + ", year=" + year + ") ===");
            String shopId = getShopId(token);
            
            // Use current month/year if not provided or if 0
            boolean useFilter = month != null && year != null && month > 0 && year > 0;
            
            var result = useFilter 
                ? orderRepository.getCategoryRevenueDataByMonth(shopId, month, year)
                : orderRepository.getCategoryRevenueData(shopId);
            
            System.out.println("Category Chart Data Count: " + result.size());
            System.out.println("==========================================================================\n");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("ERROR in /category-chart: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
