package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.ShopDto;
import com.fashion.auth.model.Shop;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShopController {

    private final ShopRepository shopRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public ShopController(ShopRepository shopRepository, JwtUtils jwtUtils, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
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
}
