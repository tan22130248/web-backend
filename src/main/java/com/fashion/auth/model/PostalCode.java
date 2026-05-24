package com.fashion.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "postal_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class PostalCode {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "char(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "area_code", length = 20)
    private String areaCode;

    @Column(name = "city_district_value", length = 255)
    private String cityDistrictValue;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
