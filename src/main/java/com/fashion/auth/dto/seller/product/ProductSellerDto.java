package com.fashion.auth.dto.seller.product;

import com.fashion.auth.model.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductSellerDto {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String conditionStatus;
    private boolean isActive;
    private String status; // 🔥 Thêm trường status để phân loại Tab trên giao diện: ACTIVE, OUT_OF_STOCK, HIDDEN
    private int soldCount;
    private LocalDateTime createdAt;

    // Image info
    private String imageUrl;
    private List<String> images;

    // Flattened category info
    private String categoryId;
    private String categoryName;

    public static ProductSellerDto from(Product p) {
        ProductSellerDto dto = new ProductSellerDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setStock(p.getStock());
        dto.setConditionStatus(p.getConditionStatus());
        dto.setActive(p.isActive());
        dto.setSoldCount(p.getSoldCount());
        dto.setCreatedAt(p.getCreatedAt());

        // 🔥 LOGIC TÍNH TOÁN TRẠNG THÁI CHO SELLER DASHBOARD
        if (!p.isActive()) {
            dto.setStatus("HIDDEN");        // Đã ẩn (Sản phẩm ở Tab đã ẩn)
        } else if (p.getStock() <= 0) {
            dto.setStatus("OUT_OF_STOCK");  // Hết hàng (Sản phẩm ở Tab hết hàng)
        } else {
            dto.setStatus("ACTIVE");        // Đang bán (Sản phẩm ở Tab đang bán)
        }

        // Ép phẳng thông tin Category
        if (p.getCategory() != null) {
            dto.setCategoryId(p.getCategory().getId());
            dto.setCategoryName(p.getCategory().getName());
        }

        // Xử lý danh sách ảnh mượt mà tương tự cấu trúc cũ của bạn
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            dto.setImages(p.getImages().stream()
                    .map(com.fashion.auth.model.ProductImage::getImageUrl)
                    .toList());

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