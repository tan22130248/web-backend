package com.fashion.auth.dto.admin;

import com.fashion.auth.model.SellerRegistration;

import java.time.LocalDateTime;

public class SellerRegistrationResponse {
    private final String id;
    private final String userId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String address;
    private final String cccdFrontUrl;
    private final String cccdBackUrl;
    private final String status;
    private final String rejectionReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public SellerRegistrationResponse(SellerRegistration registration) {
        this.id = registration.getId();
        this.userId = registration.getUser() != null ? registration.getUser().getId() : null;
        this.fullName = registration.getFullName();
        this.email = registration.getEmail();
        this.phone = registration.getPhone();
        this.address = registration.getAddress();
        this.cccdFrontUrl = registration.getCccdFrontUrl();
        this.cccdBackUrl = registration.getCccdBackUrl();
        this.status = registration.getStatus() != null ? registration.getStatus().name() : null;
        this.rejectionReason = registration.getRejectionReason();
        this.createdAt = registration.getCreatedAt();
        this.updatedAt = registration.getUpdatedAt();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCccdFrontUrl() {
        return cccdFrontUrl;
    }

    public String getCccdBackUrl() {
        return cccdBackUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
