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

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank
        private String emailOrPhone;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String emailOrPhone;
        @NotBlank
        private String otp;
        @NotBlank
        @Size(min = 6)
        private String newPassword;
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
        private String fullName;
        private String email;
        private String phone;
        private String role;
        private String avatarUrl;

        public UserInfo(com.fashion.auth.model.User u) {
            this.id = u.getId();
            this.username = u.getFullName();
            this.fullName = u.getFullName();
            this.email = u.getEmail();
            this.phone = u.getPhone();
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

    @Data
    public static class ForgotPasswordResponse {
        private String id;
        private String message;

        public ForgotPasswordResponse(String id) {
            this.id = id;
            this.message = "OTP đã được gửi. Vui lòng kiểm tra email hoặc tin nhắn của bạn.";
        }
    }
}
