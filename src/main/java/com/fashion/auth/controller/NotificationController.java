package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.model.Notification;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.NotificationRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository,
                                  JwtUtils jwtUtils,
                                  UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    /** GET /api/notifications */
    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserId(token);
            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/notifications/unread-count */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserId(token);
            long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/notifications/{id}/read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            String userId = getUserId(token);
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Thông báo không tồn tại"));

            if (!notification.getUser().getId().equals(userId)) {
                throw new RuntimeException("Không có quyền");
            }

            notification.setRead(true);
            notificationRepository.save(notification);
            return ResponseEntity.ok(new MessageResponse("Đã đánh dấu đã đọc"));
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
