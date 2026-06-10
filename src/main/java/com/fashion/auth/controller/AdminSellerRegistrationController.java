package com.fashion.auth.controller;

import com.fashion.auth.dto.admin.SellerRegistrationActionRequest;
import com.fashion.auth.dto.admin.SellerRegistrationResponse;
import com.fashion.auth.exception.AdminAccessException;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.service.SellerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/seller-registrations")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminSellerRegistrationController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final SellerService sellerService;

    public AdminSellerRegistrationController(UserRepository userRepository,
                                             JwtUtils jwtUtils,
                                             SellerService sellerService) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.sellerService = sellerService;
    }

    @GetMapping
    public ResponseEntity<?> listRegistrations(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        try {
            requireAdmin(token);
            String keyword = q == null ? "" : q.trim();
            String statusFilter = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);

            List<SellerRegistrationResponse> registrations = sellerService.listRegistrations(keyword, statusFilter).stream()
                    .map(SellerRegistrationResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(registrations);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRegistration(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            return ResponseEntity.ok(new SellerRegistrationResponse(sellerService.getRegistrationById(id)));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveRegistration(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            return ResponseEntity.ok(new SellerRegistrationResponse(sellerService.approveRegistration(id)));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectRegistration(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody SellerRegistrationActionRequest request) {
        try {
            requireAdmin(token);
            return ResponseEntity.ok(new SellerRegistrationResponse(sellerService.rejectRegistration(id, request.getReason())));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRegistration(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            sellerService.deleteRegistration(id);
            return ResponseEntity.ok().body(new java.util.HashMap<String, String>() {{ put("message", "Đã xóa đăng ký người bán"); }});
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

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new java.util.HashMap<String, String>() {{ put("message", message); }});
    }
}
