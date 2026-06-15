package com.fashion.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class WeeklyShopDTO {
    private String id;
    private String name;
    private BigDecimal rating;
    private String description;
    private String imageUrl;
    private Integer totalSold;
    private Integer responseRate;
    private String tier;
}
