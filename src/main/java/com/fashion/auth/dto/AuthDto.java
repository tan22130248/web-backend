package com.fashion.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

public class AuthDto {

    // ── Requests ──────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;
        @NotBlank
        @Size(min = 6)
        private String password;
        @NotBlank
        private String confirmPassword;
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(min = 6, max = 6)
        private String otp;
    }

    @Data
    public static class SendOtpRequest {
        @NotBlank
        private String email;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank
        @Email
        private String email;
        @NotBlank
        private String otp;
    }

    // ── Responses ─────────────────────────────────────────────

    @Data
    public static class AuthResponse {
        private String token;
        private UserInfo user;

        public AuthResponse(String token, UserInfo user) {
            this.token = token;
            this.user = user;
        }
    }

    @Data
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        private String role;
        private String avatarUrl;

        public UserInfo(com.fashion.auth.model.User u) {
            this.id = u.getId();
            this.username = u.getFullName();
            this.email = u.getEmail();
            this.role = u.getRole().name();
            this.avatarUrl = u.getAvatarUrl();
        }
    }

    @Data
    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }
}
