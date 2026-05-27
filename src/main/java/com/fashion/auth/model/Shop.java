package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Shop {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "shop_name", nullable = false, length = 150)
    private String shopName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private int totalPoints = 0;

    @Column(name = "total_sold", nullable = false)
    @Builder.Default
    private int totalSold = 0;

    @Column(name = "avg_rating", nullable = false, precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "ghn_token", length = 255)
    private String ghnToken;

    @Column(name = "ghn_shop_id")
    private Integer ghnShopId;

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
