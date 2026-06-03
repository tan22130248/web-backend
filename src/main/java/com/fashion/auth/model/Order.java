package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.pending;

    @Column(name = "order_code", nullable = false, length = 30)
    private String orderCode;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "to_district_id")
    private Integer toDistrictId;

    @Column(name = "to_ward_code", length = 50)
    private String toWardCode;

    @Column(name = "ghn_tracking_code", length = 50)
    private String ghnTrackingCode;

    @Column(columnDefinition = "TEXT")
    private String note;


    @Column(length = 20)
    @Builder.Default
    private String type = "cod";

    @Column(name = "payment_method", length = 50)
    @Builder.Default
    private String paymentMethod = "cod";

    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private String paymentStatus = "unpaid";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum OrderStatus {
        pending,
        confirmed,
        shipping,
        delivered,
        cancelled,
        refunded
    }
}
