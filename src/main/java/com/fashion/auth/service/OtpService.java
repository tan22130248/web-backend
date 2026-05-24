package com.fashion.auth.service;

import com.fashion.auth.model.OtpEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final JavaMailSender mailSender;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.ttl-minutes:10}")
    private int ttlMinutes;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Generate and email a 6-digit OTP */
    public void sendOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(ttlMinutes)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("FashionHub – Mã xác thực OTP");
        msg.setText(
                "Xin chào!\n\n" +
                        "Mã OTP đăng ký tài khoản FashionHub của bạn là:\n\n" +
                        "  " + otp + "\n\n" +
                        "Mã có hiệu lực trong " + ttlMinutes + " phút.\n" +
                        "Không chia sẻ mã này với bất kỳ ai.\n\n" +
                        "Trân trọng,\nFashionHub Team");
        mailSender.send(msg);
    }

    /** Verify OTP – returns true if valid, removes entry on success */
    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || entry.isExpired()) {
            otpStore.remove(email);
            return false;
        }
        if (entry.getOtp().equals(otp)) {
            otpStore.remove(email);
            return true;
        }
        return false;
    }

    /** Check without consuming (used before registration) */
    public boolean isOtpValid(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        return entry != null && !entry.isExpired() && entry.getOtp().equals(otp);
    }
}
