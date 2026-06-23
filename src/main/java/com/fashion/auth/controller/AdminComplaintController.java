package com.fashion.auth.controller;

import com.fashion.auth.dto.admin.AdminComplaintResponse;
import com.fashion.auth.exception.AdminAccessException;
import com.fashion.auth.model.Review;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.ReviewRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin complaint management — "Quản lý khiếu nại".
 *
 * The system has no dedicated complaint/ticket entity, so this controller maps
 * complaints onto low-rating product reviews (rating &lt;= 3). It is read-only:
 * admins can list, filter and view complaints, but the underlying reviews are
 * not mutated (reviews carry no status column). Follows the manual Bearer-check
 * pattern of the other admin controllers.
 */
@RestController
@RequestMapping("/api/admin/complaints")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminComplaintController {

    /** Reviews at or below this rating are surfaced as complaints. */
    private static final int COMPLAINT_RATING_THRESHOLD = 3;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AdminComplaintController(ReviewRepository reviewRepository,
                                    UserRepository userRepository,
                                    JwtUtils jwtUtils) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    /** GET /api/admin/complaints?q=&priority= */
    @GetMapping
    public ResponseEntity<?> getComplaints(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String priority) {
        try {
            requireAdmin(token);

            String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
            String priorityFilter = priority == null ? "all" : priority.trim().toLowerCase(Locale.ROOT);

            List<AdminComplaintResponse> complaints = reviewRepository.findAll().stream()
                    .filter(r -> r.getRating() <= COMPLAINT_RATING_THRESHOLD)
                    .map(AdminComplaintResponse::new)
                    .filter(c -> keyword.isEmpty()
                            || safeLower(c.getSubject()).contains(keyword)
                            || safeLower(c.getContent()).contains(keyword)
                            || safeLower(c.getReporterName()).contains(keyword)
                            || safeLower(c.getShopName()).contains(keyword))
                    .filter(c -> "all".equals(priorityFilter) || priorityFilter.isEmpty()
                            || priorityFilter.equals(c.getPriority()))
                    .sorted(Comparator.comparing(AdminComplaintResponse::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();

            return ResponseEntity.ok(complaints);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** GET /api/admin/complaints/stats — counts for dashboard cards */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String token) {
        try {
            requireAdmin(token);

            List<Review> complaints = reviewRepository.findAll().stream()
                    .filter(r -> r.getRating() <= COMPLAINT_RATING_THRESHOLD)
                    .toList();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", complaints.size());
            stats.put("high", complaints.stream().filter(r -> r.getRating() <= 1).count());
            stats.put("medium", complaints.stream().filter(r -> r.getRating() == 2).count());
            stats.put("low", complaints.stream().filter(r -> r.getRating() == 3).count());
            return ResponseEntity.ok(stats);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** GET /api/admin/complaints/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaint(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Review review = reviewRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Khiếu nại không tồn tại"));
            return ResponseEntity.ok(new AdminComplaintResponse(review));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

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
