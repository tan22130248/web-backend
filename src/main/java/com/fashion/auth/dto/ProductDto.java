package com.fashion.auth.dto;

import com.fashion.auth.model.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String conditionStatus;
    private boolean isActive;
    private int soldCount;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Image info
    private String imageUrl;
    private List<String> images;

    // Flattened shop info
    private String shopId;
    private String shopName;
    private String shopAvatarUrl;
    private int shopTotalPoints;

    // Flattened category info
    private String categoryId;
    private String categoryName;
    private String categorySlug;

    public static ProductDto from(Product p) {
        ProductDto dto = new ProductDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setStock(p.getStock());
        dto.setConditionStatus(p.getConditionStatus());
        dto.setActive(p.isActive());
        dto.setSoldCount(p.getSoldCount());
        dto.setViewCount(p.getViewCount());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());

        if (p.getShop() != null) {
            dto.setShopId(p.getShop().getId());
            dto.setShopName(p.getShop().getShopName());
            dto.setShopAvatarUrl(p.getShop().getAvatarUrl());
            dto.setShopTotalPoints(p.getShop().getTotalPoints());
        }
        if (p.getCategory() != null) {
            dto.setCategoryId(p.getCategory().getId());
            dto.setCategoryName(p.getCategory().getName());
            dto.setCategorySlug(p.getCategory().getSlug());
        }

        if (p.getImages() != null && !p.getImages().isEmpty()) {
            dto.setImages(p.getImages().stream().map(com.fashion.auth.model.ProductImage::getImageUrl).toList());
            
            // Prefer primary image, else first image
            String primaryImage = p.getImages().stream()
                    .filter(com.fashion.auth.model.ProductImage::isPrimary)
                    .map(com.fashion.auth.model.ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(p.getImages().get(0).getImageUrl());
            dto.setImageUrl(primaryImage);
        } else {
            dto.setImages(java.util.Collections.emptyList());
        }

        return dto;
    }
}
