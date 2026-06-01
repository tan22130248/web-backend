package com.fashion.auth.service;

import com.fashion.auth.dto.AuthDto.*;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
    }

    public AuthResponse login(LoginRequest req) {
        log.info("Login attempt for: {}", req.getUsername());

        User user = userRepository.findByFullName(req.getUsername())
                .or(() -> userRepository.findByEmail(req.getUsername()))
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (user.getPasswordHash().startsWith("{oauth2}")) {
            throw new RuntimeException("Tài khoản này đăng nhập qua Google/Facebook, vui lòng dùng nút đăng nhập mạng xã hội");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản đã bị khoá");
        }

        String token = jwtUtils.generateToken(user.getFullName(), user.getEmail(), user.getRole().name());
        log.info("Login success for: {}", req.getUsername());
        return new AuthResponse(token, new UserInfo(user));
    }

    @Transactional
    public MessageResponse register(RegisterRequest req) {
        log.info("Register attempt - username: {}, email: {}", req.getUsername(), req.getEmail());

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu nhập lại không khớp");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        if (userRepository.existsByFullName(req.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        if (!otpService.verifyOtp(req.getEmail(), req.getOtp())) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        try {
            User user = User.builder()
                    .fullName(req.getUsername())
                    .email(req.getEmail())
                    .passwordHash(passwordEncoder.encode(req.getPassword()))
                    .role(User.Role.buyer)
                    .isActive(true)
                    .build();

            User saved = userRepository.save(user);
            log.info("User registered successfully: id={}, username={}", saved.getId(), saved.getFullName());
            return new MessageResponse("Đăng ký thành công!");
        } catch (Exception e) {
            log.error("Failed to save user: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi lưu tài khoản: " + e.getMessage());
        }
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req) {
        log.info("Forgot password request for: {}", req.getEmailOrPhone());

        User user = userRepository.findByEmail(req.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(req.getEmailOrPhone()))
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        try {
            otpService.sendOtp(user.getEmail());
            log.info("OTP sent for forgot password: {}", user.getEmail());
            return new ForgotPasswordResponse(user.getId());
        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi gửi mã OTP: " + e.getMessage());
        }
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest req) {
        log.info("Reset password request for: {}", req.getEmailOrPhone());

        User user = userRepository.findByEmail(req.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(req.getEmailOrPhone()))
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        if (!otpService.isOtpValid(user.getEmail(), req.getOtp())) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        log.info("Password reset successfully for: {}", user.getEmail());
        return new MessageResponse("Mật khẩu đã được đặt lại thành công");
    }
}