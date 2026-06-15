package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            columnDefinition = "char(36) character set utf8mb4 collate utf8mb4_unicode_ci")
    private Order order;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(length = 30)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_by", length = 50)
    private String changedBy;

    @Column(length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
