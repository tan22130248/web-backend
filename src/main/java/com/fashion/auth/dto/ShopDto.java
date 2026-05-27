package com.fashion.auth.dto;

import com.fashion.auth.model.Shop;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShopDto {
    private String id;
    private String userId;
    private String shopName;
    private String avatarUrl;
    private String address;
    private String description;
    private boolean isVerified;
    private String ghnToken;
    private Integer ghnShopId;
    private LocalDateTime createdAt;

    public static ShopDto from(Shop s) {
        ShopDto dto = new ShopDto();
        dto.setId(s.getId());
        dto.setUserId(s.getUser().getId());
        dto.setShopName(s.getShopName());
        dto.setAvatarUrl(s.getAvatarUrl());
        dto.setAddress(s.getAddress());
        dto.setDescription(s.getDescription());
        dto.setVerified(s.isVerified());
        dto.setGhnToken(s.getGhnToken());
        dto.setGhnShopId(s.getGhnShopId());
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }
}
