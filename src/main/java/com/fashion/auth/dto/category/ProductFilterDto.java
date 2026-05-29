package com.fashion.auth.dto.category;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductFilterDto {
    private String categoryId;
    private String conditionStatus;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    private int page = 0;
    private int size = 9;
}
