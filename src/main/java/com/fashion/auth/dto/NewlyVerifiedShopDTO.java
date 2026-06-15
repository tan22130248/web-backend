package com.fashion.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NewlyVerifiedShopDTO {
    private String id;
    private String name;
    private String imageUrl;
    private String joinDate; // ISO date string
    private String joinDateRelative; // "Tham gia 2 ngày trước"
}
