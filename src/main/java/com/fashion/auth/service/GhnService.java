package com.fashion.auth.service;

import com.fashion.auth.dto.ghn.GhnBaseResponse;
import com.fashion.auth.dto.ghn.GhnFeeRequest;
import com.fashion.auth.dto.ghn.GhnFeeResponse;
import com.fashion.auth.dto.ghn.GhnShopConfig;
import com.fashion.auth.exception.GhnIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnService {

    private final RestClient ghnRestClient;
    private final GhnShopConfigStore ghnShopConfigStore;

    /**
     * Tính phí vận chuyển (Shipping Fee) thông qua GHN
     */
    public GhnFeeResponse calculateFee(String internalShopId, GhnFeeRequest request) {
        GhnShopConfig config = ghnShopConfigStore.getConfig(internalShopId)
                .orElseThrow(() -> new GhnIntegrationException("Shop " + internalShopId + " is not configured for GHN"));

        log.info("Calculating GHN shipping fee for shop: {}", internalShopId);

        // GHN requires shop_id in header for fee calculation sometimes, or in body. 
        // According to GHN docs, token is in 'Token' header, ShopId is in 'ShopId' header
        
        GhnBaseResponse<GhnFeeResponse> response = ghnRestClient.post()
                .uri("/shipping-order/fee")
                .header("Token", config.getGhnToken())
                .header("ShopId", String.valueOf(config.getGhnShopId()))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("GHN API Error. Status: {}", res.getStatusCode());
                    throw new GhnIntegrationException("GHN API Error: " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.getCode() != 200) {
            String msg = response != null ? response.getMessage() : "Unknown error";
            log.error("GHN API returned non-200 code: {}", msg);
            throw new GhnIntegrationException("Failed to calculate GHN fee: " + msg);
        }

        return response.getData();
    }
}
