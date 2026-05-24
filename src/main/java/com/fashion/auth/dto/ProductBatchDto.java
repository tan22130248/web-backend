package com.fashion.auth.dto;

import com.fashion.auth.model.Product;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Lightweight DTO for the POST /api/products/batch endpoint.
 * Returns only the fields the client-side cart needs to refresh.
 */
@Data
public class ProductBatchDto {
    private String id;
    private String name;
    private BigDecimal price;
    private int stock;
    private boolean isActive;
    private String conditionStatus;
    private String shopId;
    private String shopName;
    private String imageUrl;

    public static ProductBatchDto from(Product p) {
        ProductBatchDto dto = new ProductBatchDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPrice(p.getPrice());
        dto.setStock(p.getStock());
        dto.setActive(p.isActive());
        dto.setConditionStatus(p.getConditionStatus());
        if (p.getShop() != null) {
            dto.setShopId(p.getShop().getId());
            dto.setShopName(p.getShop().getShopName());
        }
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            String primaryImage = p.getImages().stream()
                    .filter(com.fashion.auth.model.ProductImage::isPrimary)
                    .map(com.fashion.auth.model.ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(p.getImages().get(0).getImageUrl());
            dto.setImageUrl(primaryImage);
        }
        return dto;
    }
}
