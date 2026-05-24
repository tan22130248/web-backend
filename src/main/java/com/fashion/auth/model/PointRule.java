package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class PointRule {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Column(name = "min_order_value", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "points_per_order", nullable = false)
    @Builder.Default
    private int pointsPerOrder = 10;

    @Column(name = "bonus_multiplier", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal bonusMultiplier = BigDecimal.ONE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
