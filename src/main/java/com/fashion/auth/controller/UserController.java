package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.*;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, JwtUtils jwtUtils, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token không hợp lệ"));
            }

            String jwt = token.substring(7);
            String email = jwtUtils.getEmailFromToken(jwt);

            if (email == null || !jwtUtils.validateToken(jwt)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token không hợp lệ"));
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            return ResponseEntity.ok(new UserProfileResponse(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateProfileRequest req) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token không hợp lệ"));
            }

            String jwt = token.substring(7);
            String email = jwtUtils.getEmailFromToken(jwt);

            if (email == null || !jwtUtils.validateToken(jwt)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token không hợp lệ"));
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) {
                user.setFullName(req.getFullName());
            }
            if (req.getPhone() != null && !req.getPhone().trim().isEmpty()) {
                user.setPhone(req.getPhone());
            }
            if (req.getAvatarUrl() != null && !req.getAvatarUrl().trim().isEmpty()) {
                user.setAvatarUrl(req.getAvatarUrl());
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(new UserProfileResponse(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest req) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token không hợp lệ"));
            }

            String jwt = token.substring(7);
            String email = jwtUtils.getEmailFromToken(jwt);

            if (email == null || !jwtUtils.validateToken(jwt)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token không hợp lệ"));
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Mật khẩu cũ không chính xác"));
            }

            if (!req.getNewPassword().equals(req.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Mật khẩu mới không khớp"));
            }

            if (req.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body(new MessageResponse("Mật khẩu phải có ít nhất 6 ký tự"));
            }

            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Mật khẩu đã được thay đổi thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    public static class UserProfileResponse {
        private String id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private String role;
        private boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserProfileResponse(User user) {
            this.id = user.getId();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
            this.phone = user.getPhone();
            this.avatarUrl = user.getAvatarUrl();
            this.role = user.getRole().name();
            this.isActive = user.isActive();
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
        }

        public String getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getRole() { return role; }
        public boolean isActive() { return isActive; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static class UpdateProfileRequest {
        private String fullName;
        private String phone;
        private String avatarUrl;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
        private String confirmPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
