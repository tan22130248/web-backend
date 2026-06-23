package com.fashion.auth.dto.admin;

import com.fashion.auth.model.Product;
import com.fashion.auth.model.ProductImage;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class AdminProductResponse {
    private final String id;
    private final String name;
    private final String sku;
    private final BigDecimal price;
    private final int stock;
    private final String conditionStatus;
    private final boolean isActive;
    private final int soldCount;
    private final int viewCount;
    private final String imageUrl;

    private final String shopId;
    private final String shopName;
    private final String sellerName;
    private final String sellerEmail;
    private final LocalDateTime sellerJoinedAt;

    private final String categoryName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AdminProductResponse(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        // Derive a short SKU-like code from the UUID for display purposes only
        this.sku = p.getId() != null ? p.getId().substring(0, 8).toUpperCase() : null;
        this.price = p.getPrice();
        this.stock = p.getStock();
        this.conditionStatus = p.getConditionStatus();
        this.isActive = p.isActive();
        this.soldCount = p.getSoldCount();
        this.viewCount = p.getViewCount();
        this.createdAt = p.getCreatedAt();
        this.updatedAt = p.getUpdatedAt();

        String primaryImage = null;
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            primaryImage = p.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(p.getImages().get(0).getImageUrl());
        }
        this.imageUrl = primaryImage;

        if (p.getShop() != null) {
            this.shopId = p.getShop().getId();
            this.shopName = p.getShop().getShopName();
            if (p.getShop().getUser() != null) {
                this.sellerName = p.getShop().getUser().getFullName();
                this.sellerEmail = p.getShop().getUser().getEmail();
                this.sellerJoinedAt = p.getShop().getUser().getCreatedAt();
            } else {
                this.sellerName = null;
                this.sellerEmail = null;
                this.sellerJoinedAt = null;
            }
        } else {
            this.shopId = null;
            this.shopName = null;
            this.sellerName = null;
            this.sellerEmail = null;
            this.sellerJoinedAt = null;
        }

        this.categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public String getConditionStatus() { return conditionStatus; }
    public boolean isActive() { return isActive; }
    public int getSoldCount() { return soldCount; }
    public int getViewCount() { return viewCount; }
    public String getImageUrl() { return imageUrl; }
    public String getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public String getSellerName() { return sellerName; }
    public String getSellerEmail() { return sellerEmail; }
    public LocalDateTime getSellerJoinedAt() { return sellerJoinedAt; }
    public String getCategoryName() { return categoryName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
