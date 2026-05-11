package com.fashion.auth.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    private String role = "buyer";
    private Boolean isActive = true;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
}
