package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 50)
    private String type;

    @Column(length = 20)
    private String size;

    @Column(length = 30)
    private String color;

    @Column(name = "price_modifier", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private int stock = 0;

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private int stockQty = 0;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
