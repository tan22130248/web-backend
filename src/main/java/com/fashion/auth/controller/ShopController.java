package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.FeaturedShopDTO;
import com.fashion.auth.dto.NewlyVerifiedShopDTO;
import com.fashion.auth.dto.ShopDto;
import com.fashion.auth.dto.WeeklyShopDTO;
import com.fashion.auth.model.Shop;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.model.ShopPoint;
import com.fashion.auth.service.ShopService;
import com.fashion.auth.service.ShopRankingService;
import com.fashion.auth.service.ShopRankingPopulationService;
import com.fashion.auth.service.ShopPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shops")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShopController {

    private final ShopRepository shopRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final ShopService shopService;
    private final ShopRankingService rankingService;
    private final ShopRankingPopulationService populationService;
    private final ShopPointService shopPointService;


    public ShopController(ShopRepository shopRepository, JwtUtils jwtUtils, UserRepository userRepository, ShopService shopService, ShopRankingService rankingService, ShopRankingPopulationService populationService, ShopPointService shopPointService) {
        this.shopRepository = shopRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.shopService = shopService;
        this.rankingService = rankingService;
        this.populationService = populationService;
        this.shopPointService = shopPointService;
    }

    /** GET /api/shops/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getShop(@PathVariable String id) {
        try {
            Shop shop = shopRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại"));
            return ResponseEntity.ok(ShopDto.from(shop));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/shops/my */
    @GetMapping("/my")
    public ResponseEntity<?> getMyShop(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserId(token);
            Shop shop = shopRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));
            return ResponseEntity.ok(ShopDto.from(shop));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/shops */
    @PostMapping
    public ResponseEntity<?> createShop(
            @RequestHeader("Authorization") String token,
            @RequestBody Shop shopData) {
        try {
            String userId = getUserId(token);
            if (shopRepository.existsByUserId(userId)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Bạn đã có cửa hàng rồi"));
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            Shop shop = Shop.builder()
                    .user(user)
                    .shopName(shopData.getShopName())
                    .avatarUrl(shopData.getAvatarUrl())
                    .address(shopData.getAddress())
                    .description(shopData.getDescription())
                    .build();

            return ResponseEntity.ok(ShopDto.from(shopRepository.save(shop)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PUT /api/shops */
    @PutMapping
    public ResponseEntity<?> updateShop(
            @RequestHeader("Authorization") String token,
            @RequestBody Shop shopData) {
        try {
            String userId = getUserId(token);
            Shop shop = shopRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));

            if (shopData.getShopName() != null) shop.setShopName(shopData.getShopName());
            if (shopData.getAvatarUrl() != null) shop.setAvatarUrl(shopData.getAvatarUrl());
            if (shopData.getAddress() != null) shop.setAddress(shopData.getAddress());
            if (shopData.getDescription() != null) shop.setDescription(shopData.getDescription());
            if (shopData.getGhnToken() != null) shop.setGhnToken(shopData.getGhnToken());
            if (shopData.getGhnShopId() != null) shop.setGhnShopId(shopData.getGhnShopId());

            return ResponseEntity.ok(ShopDto.from(shopRepository.save(shop)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/shops/my/points — tổng điểm + lịch sử */
    @GetMapping("/my/points")
    public ResponseEntity<?> getMyPoints(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserId(token);
            Shop shop = shopRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));

            int totalPoints = shopPointService.getTotalPoints(shop.getId());
            List<ShopPoint> history = shopPointService.getPointHistory(shop.getId());

            List<Map<String, Object>> historyList = history.stream().map(sp -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", sp.getId());
                item.put("pointsEarned", sp.getPointsEarned());
                item.put("type", sp.getType());
                item.put("reason", sp.getReason());
                item.put("amount", sp.getAmount());
                item.put("createdAt", sp.getCreatedAt());
                return item;
            }).toList();

            return ResponseEntity.ok(Map.of(
                    "totalPoints", totalPoints,
                    "shopTotalPoints", shop.getTotalPoints(),
                    "history", historyList
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private String getUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ");
        }
        String jwt = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(jwt);
        if (email == null || !jwtUtils.validateToken(jwt)) {
            throw new RuntimeException("Token không hợp lệ");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return user.getId();
    }


    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false, defaultValue = "") String q) {
        List<FeaturedShopDTO> data = shopService.searchShops(q);
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        resp.put("count", data.size());
        return resp;
    }
    @GetMapping("/featured")
    public Map<String, Object> featured() {
        List<FeaturedShopDTO> data = shopService.getFeaturedShops();
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        resp.put("count", data.size());
        return resp;
    }

    @GetMapping("/featured/week")
    public Map<String, Object> week() {
        WeeklyShopDTO data = shopService.getWeeklyShop();
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        return resp;
    }

    @GetMapping("/newly-verified")
    public Map<String, Object> newlyVerified() {
        List<NewlyVerifiedShopDTO> data = shopService.getNewlyVerifiedShops();
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        resp.put("count", data.size());
        return resp;
    }

    /**
     * DEBUG: Manually trigger ranking calculation
     * Usage: POST /api/shops/ranking/calculate
     */
    @PostMapping("/ranking/calculate")
    public ResponseEntity<?> calculateRankings() {
        try {
            rankingService.calculateRankingsForCurrentMonth();
            return ResponseEntity.ok(Map.of(
                "message", "Rankings calculated successfully",
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DEBUG: Populate sample shop rankings from CSV data
     * Usage: POST /api/shops/ranking/populate-sample?count=50
     */
    @PostMapping("/ranking/populate-sample")
    public ResponseEntity<?> populateSampleRankings(
            @RequestParam(defaultValue = "50") int count) {
        try {
            populationService.populateSampleRankings(count);
            long totalNow = populationService.getTotalRankings();
            return ResponseEntity.ok(Map.of(
                "message", "Sample rankings populated successfully",
                "count", count,
                "totalRankingsInDB", totalNow,
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DEBUG: Check shop ranking data
     * Usage: GET /api/shops/ranking/debug
     */
    @GetMapping("/ranking/debug")
    public ResponseEntity<?> debugRankings() {
        try {
            List<FeaturedShopDTO> featured = shopService.getFeaturedShops();
            WeeklyShopDTO weekly = shopService.getWeeklyShop();
            List<NewlyVerifiedShopDTO> newlyVerified = shopService.getNewlyVerifiedShops();
            long totalRankings = populationService.getTotalRankings();
            
            return ResponseEntity.ok(Map.of(
                "totalRankingsInDB", totalRankings,
                "featuredShopsCount", featured.size(),
                "featuredShops", featured,
                "weeklyShop", weekly,
                "newlyVerifiedCount", newlyVerified.size(),
                "newlyVerified", newlyVerified
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        }
    }
}
