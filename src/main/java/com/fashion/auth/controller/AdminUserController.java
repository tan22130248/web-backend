package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.admin.CreateUserRequest;
import com.fashion.auth.dto.admin.UpdateStatusRequest;
import com.fashion.auth.dto.admin.UpdateUserRequest;
import com.fashion.auth.dto.admin.UserResponse;
import com.fashion.auth.exception.AdminAccessException;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminUserController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository,
                               JwtUtils jwtUtils,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive) {
        try {
            requireAdmin(token);

            String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
            String roleFilter = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);

            List<UserResponse> users = userRepository.findAll().stream()
                    .filter(user -> keyword.isEmpty()
                            || safeLower(user.getFullName()).contains(keyword)
                            || safeLower(user.getEmail()).contains(keyword)
                            || safeLower(user.getPhone()).contains(keyword))
                    .filter(user -> roleFilter.isEmpty()
                            || "all".equals(roleFilter)
                            || user.getRole().name().equals(roleFilter))
                    .filter(user -> isActive == null || user.isActive() == isActive)
                    .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(UserResponse::new)
                    .toList();

            return ResponseEntity.ok(users);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            User user = findUser(id);
            return ResponseEntity.ok(new UserResponse(user));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateUserRequest req) {
        try {
            requireAdmin(token);
            validateUniqueEmail(req.getEmail(), null);
            validateUniqueFullName(req.getFullName(), null);

            User user = User.builder()
                    .fullName(req.getFullName().trim())
                    .email(req.getEmail().trim())
                    .phone(blankToNull(req.getPhone()))
                    .avatarUrl(blankToNull(req.getAvatarUrl()))
                    .passwordHash(passwordEncoder.encode(req.getPassword()))
                    .role(parseRole(req.getRole()))
                    .isActive(req.getIsActive() == null || req.getIsActive())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User saved = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(saved));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest req) {
        try {
            User admin = requireAdmin(token);
            User user = findUser(id);

            validateUniqueEmail(req.getEmail(), user.getId());
            validateUniqueFullName(req.getFullName(), user.getId());

            User.Role nextRole = parseRole(req.getRole());
            boolean nextActive = req.getIsActive() == null || req.getIsActive();

            if (admin.getId().equals(user.getId()) && (nextRole != User.Role.admin || !nextActive)) {
                throw new RuntimeException("Không thể tự hạ quyền hoặc khóa tài khoản admin đang đăng nhập");
            }

            user.setFullName(req.getFullName().trim());
            user.setEmail(req.getEmail().trim());
            user.setPhone(blankToNull(req.getPhone()));
            user.setAvatarUrl(blankToNull(req.getAvatarUrl()));
            user.setRole(nextRole);
            user.setActive(nextActive);
            user.setUpdatedAt(LocalDateTime.now());

            return ResponseEntity.ok(new UserResponse(userRepository.save(user)));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest req) {
        try {
            User admin = requireAdmin(token);
            User user = findUser(id);

            if (admin.getId().equals(user.getId()) && !req.isActive()) {
                throw new RuntimeException("Không thể tự khóa tài khoản admin đang đăng nhập");
            }

            user.setActive(req.isActive());
            user.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(new UserResponse(userRepository.save(user)));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            User admin = requireAdmin(token);
            User user = findUser(id);

            if (admin.getId().equals(user.getId())) {
                throw new RuntimeException("Không thể tự xóa tài khoản admin đang đăng nhập");
            }

            userRepository.delete(user);
            return ResponseEntity.ok(new MessageResponse("Đã xóa người dùng"));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
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

    private User findUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }

    private void validateUniqueEmail(String email, String currentUserId) {
        userRepository.findByEmail(email.trim()).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        });
    }

    private void validateUniqueFullName(String fullName, String currentUserId) {
        userRepository.findByFullName(fullName.trim()).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new RuntimeException("Tên người dùng đã tồn tại");
            }
        });
    }

    private User.Role parseRole(String role) {
        if (role == null || role.isBlank()) return User.Role.buyer;

        try {
            return User.Role.valueOf(role.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Vai trò không hợp lệ");
        }
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static ResponseEntity<MessageResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new MessageResponse(message));
    }

}
