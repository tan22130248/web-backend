package com.fashion.auth.controller;

import com.fashion.auth.dto.admin.AdminProductResponse;
import com.fashion.auth.exception.AdminAccessException;
import com.fashion.auth.model.Product;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.ProductRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;

/**
 * Admin product moderation.
 * Mirrors AdminSellerRegistrationController: manual Bearer check via requireAdmin,
 * Vietnamese messages, MessageResponse-style bodies.
 *
 * Status model (mapped onto existing Product fields, no schema change):
 *  - "active"   → isActive = true  AND stock > 0     (Đang bán)
 *  - "pending"  → isActive = true  AND stock == 0    (Chờ duyệt / chưa lên kệ)
 *  - "violation"→ isActive = false                   (Vi phạm / đã gỡ)
 */
@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminProductController {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AdminProductController(ProductRepository productRepository,
                                  UserRepository userRepository,
                                  JwtUtils jwtUtils) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        try {
            requireAdmin(token);

            String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
            String statusFilter = status == null ? "all" : status.trim().toLowerCase(Locale.ROOT);

            List<AdminProductResponse> products = productRepository.findAll().stream()
                    .filter(p -> keyword.isEmpty()
                            || safeLower(p.getName()).contains(keyword)
                            || (p.getId() != null && p.getId().toLowerCase(Locale.ROOT).contains(keyword)))
                    .filter(p -> matchStatus(p, statusFilter))
                    .sorted(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(AdminProductResponse::new)
                    .toList();

            return ResponseEntity.ok(products);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String token) {
        try {
            requireAdmin(token);
            List<Product> all = productRepository.findAll();
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", all.size());
            stats.put("active", all.stream().filter(p -> matchStatus(p, "active")).count());
            stats.put("pending", all.stream().filter(p -> matchStatus(p, "pending")).count());
            stats.put("violation", all.stream().filter(p -> matchStatus(p, "violation")).count());
            return ResponseEntity.ok(stats);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Product product = findProduct(id);
            return ResponseEntity.ok(new AdminProductResponse(product));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** PATCH /api/admin/products/{id}/approve — duyệt sản phẩm (cho lên kệ) */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Product product = findProduct(id);
            product.setActive(true);
            product.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(new AdminProductResponse(productRepository.save(product)));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** PATCH /api/admin/products/{id}/violation — đánh dấu vi phạm (gỡ khỏi sàn) */
    @PatchMapping("/{id}/violation")
    public ResponseEntity<?> flagViolation(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Product product = findProduct(id);
            product.setActive(false);
            product.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(new AdminProductResponse(productRepository.save(product)));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Product product = findProduct(id);
            productRepository.delete(product);
            return ResponseEntity.ok(message("Đã xóa sản phẩm"));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private boolean matchStatus(Product p, String statusFilter) {
        if (statusFilter == null || statusFilter.isEmpty() || "all".equals(statusFilter)) {
            return true;
        }
        return switch (statusFilter) {
            case "active" -> p.isActive() && p.getStock() > 0;
            case "pending" -> p.isActive() && p.getStock() == 0;
            case "violation" -> !p.isActive();
            default -> true;
        };
    }

    private Product findProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
    }

    private User requireAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AdminAccessException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        String jwt = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(jwt);

        if (email == null || !jwtUtils.validateToken(jwt)) {
            throw new AdminAccessException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AdminAccessException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));

        if (!user.isActive()) {
            throw new AdminAccessException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        if (user.getRole() != User.Role.admin) {
            throw new AdminAccessException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản trị");
        }

        return user;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> message(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return body;
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message(message));
    }
}
