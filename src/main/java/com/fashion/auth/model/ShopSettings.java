package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "shop_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ShopSettings {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private Shop shop;

    @Column(length = 50)
    private String tier;

    @Column(name = "auto_confirm", nullable = false)
    @Builder.Default
    private boolean autoConfirm = false;
}
