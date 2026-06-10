package com.fashion.auth.service;

import com.fashion.auth.dto.seller.SellerRegistrationRequestDto;
import com.fashion.auth.model.SellerRegistration;
import com.fashion.auth.model.Shop;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.SellerRegistrationRepository;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class SellerService {
    private static final Logger log = LoggerFactory.getLogger(SellerService.class);

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public SellerService(UserRepository userRepository, ShopRepository shopRepository, SellerRegistrationRepository sellerRegistrationRepository, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void registerAsSeller(String userEmail, SellerRegistrationRequestDto request) {
        log.info("Registering seller for email: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getRole() == User.Role.seller) {
            throw new RuntimeException("Bạn đã là người bán");
        }

        var existingRegistration = sellerRegistrationRepository.findByUserId(user.getId());
        if (existingRegistration.isPresent()) {
            SellerRegistration registration = existingRegistration.get();
            if (registration.getStatus() != SellerRegistration.RegistrationStatus.rejected) {
                throw new RuntimeException("Bạn đã gửi đơn đăng ký trước đó");
            }

            registration.setFullName(request.getFullName());
            registration.setPhone(request.getPhone());
            registration.setEmail(request.getEmail());
            registration.setAddress(request.getAddress());
            registration.setCccdFrontUrl(request.getCccdFrontUrl());
            registration.setCccdBackUrl(request.getCccdBackUrl());
            registration.setStatus(SellerRegistration.RegistrationStatus.pending);
            registration.setRejectionReason(null);
            sellerRegistrationRepository.save(registration);

            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            userRepository.save(user);

            log.info("Seller registration re-submitted with status=pending for email: {}", userEmail);
            return;
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        userRepository.save(user);

        SellerRegistration registration = SellerRegistration.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .cccdFrontUrl(request.getCccdFrontUrl())
                .cccdBackUrl(request.getCccdBackUrl())
                .status(SellerRegistration.RegistrationStatus.pending)
                .build();

        sellerRegistrationRepository.save(registration);

        log.info("Seller registration created with status=pending for email: {}", userEmail);
    }

    public boolean isUserSeller(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return user.getRole() == User.Role.seller;
    }

    public String getSellerRegistrationStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (user.getRole() == User.Role.seller) {
            return "seller";
        }

        var registration = sellerRegistrationRepository.findByUserId(user.getId());
        if (registration.isPresent()) {
            return registration.get().getStatus().toString();
        }

        return "none";
    }

    public List<SellerRegistration> listRegistrations(String keyword, String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);

        return sellerRegistrationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(registration -> normalizedKeyword.isEmpty()
                        || registration.getFullName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || registration.getEmail().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(registration -> normalizedStatus.isEmpty()
                        || "all".equals(normalizedStatus)
                        || registration.getStatus().name().equals(normalizedStatus))
                .collect(Collectors.toList());
    }

    public SellerRegistration getRegistrationById(String id) {
        return sellerRegistrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Đăng ký người bán không tồn tại"));
    }

    @Transactional
    public SellerRegistration approveRegistration(String id) {
        SellerRegistration registration = getRegistrationById(id);
        if (registration.getStatus() == SellerRegistration.RegistrationStatus.approved) {
            return registration;
        }

        registration.setStatus(SellerRegistration.RegistrationStatus.approved);
        registration.setRejectionReason(null);

        User user = registration.getUser();
        user.setRole(User.Role.seller);
        userRepository.save(user);

        log.info("Seller registration approved for user={} registrationId={}", user.getEmail(), id);
        return sellerRegistrationRepository.save(registration);
    }

    @Transactional
    public SellerRegistration rejectRegistration(String id, String rejectionReason) {
        SellerRegistration registration = getRegistrationById(id);
        registration.setStatus(SellerRegistration.RegistrationStatus.rejected);
        registration.setRejectionReason(rejectionReason == null ? null : rejectionReason.trim());
        SellerRegistration saved = sellerRegistrationRepository.save(registration);

        log.info("Seller registration rejected for registrationId={} reason={}", id, rejectionReason);
        try {
            sendRejectionEmail(saved);
        } catch (Exception ex) {
            log.warn("Failed to send seller rejection email for registrationId={}: {}", id, ex.getMessage(), ex);
        }

        return saved;
    }

    private void sendRejectionEmail(SellerRegistration registration) {
        String toEmail = registration.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            msg.setFrom(fromEmail);
        }
        msg.setTo(toEmail);
        msg.setSubject("FashionHub - Đăng ký người bán bị từ chối");
        msg.setText(
                "Xin chào " + registration.getFullName() + ",\n\n" +
                "Đơn đăng ký người bán của bạn đã bị từ chối.\n\n" +
                "Lý do: " + (registration.getRejectionReason() == null ? "(chưa có)" : registration.getRejectionReason()) + "\n\n" +
                "Bạn có thể chỉnh sửa thông tin và gửi lại hồ sơ.\n" +
                "Nếu cần hỗ trợ thêm, vui lòng phản hồi lại email này.\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ FashionHub"
        );
        mailSender.send(msg);
    }

    @Transactional
    public void deleteRegistration(String id) {
        SellerRegistration registration = getRegistrationById(id);
        sellerRegistrationRepository.delete(registration);
        log.info("Seller registration deleted registrationId={}", id);
    }
}
