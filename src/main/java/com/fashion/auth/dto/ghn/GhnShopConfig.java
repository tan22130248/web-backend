package com.fashion.auth.dto.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnShopConfig {
    private String ghnToken;
    private Integer ghnShopId;
}
