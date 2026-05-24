package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.exception.AdminAccessException;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tools")
@CrossOrigin(origins = "*", maxAge = 3600)
@Validated
public class AdminToolController {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminToolController(JwtUtils jwtUtils,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/hash-password")
    public ResponseEntity<?> hashPassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody HashPasswordRequest req) {
        try {
            // requireAdmin(token);
            String hash = passwordEncoder.encode(req.getPassword());
            return ResponseEntity.ok(new HashPasswordResponse(hash));
        } catch (AdminAccessException e) {
            return ResponseEntity.status(e.getStatus()).body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
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

    @Data
    public static class HashPasswordRequest {
        @NotBlank
        private String password;
    }

    @Data
    public static class HashPasswordResponse {
        private final String passwordHash;
    }
}
