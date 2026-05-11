package com.fashion.auth.dto.admin;

import com.fashion.auth.model.User;

import java.time.LocalDateTime;

public class UserResponse {
    private final String id;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String avatarUrl;
    private final String role;
    private final boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.avatarUrl = user.getAvatarUrl();
        this.role = user.getRole().name();
        this.isActive = user.isActive();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getRole() { return role; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
