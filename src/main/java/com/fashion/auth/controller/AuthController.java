package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.*;
import com.fashion.auth.service.AuthService;
import com.fashion.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService  otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService  = otpService;
    }

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            AuthResponse resp = authService.login(req);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/auth/register */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            MessageResponse resp = authService.register(req);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/auth/send-otp  { "email": "..." } */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        try {
            otpService.sendOtp(req.getEmail());
            return ResponseEntity.ok(new MessageResponse("OTP đã được gửi đến " + req.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Gửi OTP thất bại: " + e.getMessage()));
        }
    }

    /** POST /api/auth/verify-otp  { "email": "...", "otp": "..." } */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        boolean valid = otpService.isOtpValid(req.getEmail(), req.getOtp());
        if (valid) return ResponseEntity.ok(new MessageResponse("OTP hợp lệ"));
        return ResponseEntity.badRequest().body(new MessageResponse("OTP không hợp lệ hoặc đã hết hạn"));
    }
}
