package com.fashion.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class FeaturedShopDTO {
    private String id;
    private String name;
    private BigDecimal rating;
    private String badge;
    private String category;
    private String imageUrl;
    private String tier;
    private String icon; // 🏪, 👚, 👟, etc.
}
