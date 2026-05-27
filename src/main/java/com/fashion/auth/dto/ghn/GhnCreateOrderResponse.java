package com.fashion.auth.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GhnCreateOrderResponse {
    
    @JsonProperty("order_code")
    private String orderCode;
    
    @JsonProperty("sort_code")
    private String sortCode;
    
    private String trans_type;
    
    @JsonProperty("ward_encode")
    private String wardEncode;
    
    @JsonProperty("district_encode")
    private String districtEncode;
    
    private Fee fee;
    
    @JsonProperty("total_fee")
    private Integer totalFee;
    
    @JsonProperty("expected_delivery_time")
    private String expectedDeliveryTime;

    @Data
    public static class Fee {
        private Integer main_service;
        private Integer insurance;
        private Integer cod_fee;
        private Integer station_do;
        private Integer station_pu;
        private Integer return_fee;
        private Integer r2s;
        private Integer coupon;
    }
}
