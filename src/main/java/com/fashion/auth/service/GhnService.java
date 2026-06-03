package com.fashion.auth.service;

import com.fashion.auth.dto.ghn.GhnBaseResponse;
import com.fashion.auth.dto.ghn.GhnFeeRequest;
import com.fashion.auth.dto.ghn.GhnFeeResponse;
import com.fashion.auth.dto.ghn.GhnCreateOrderRequest;
import com.fashion.auth.dto.ghn.GhnCreateOrderResponse;
import com.fashion.auth.exception.GhnIntegrationException;
import com.fashion.auth.model.Shop;
import com.fashion.auth.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnService {

    private final RestClient ghnRestClient;
    private final ShopRepository shopRepository;

    @Value("${app.ghn.api-token:}")
    private String globalGhnToken;

    @Value("${app.ghn.api-url:https://dev-online-gateway.ghn.vn/shiip/public-api/v2/}")
    private String ghnApiUrl;

    private String getMasterDataUrl(String path) {
        return ghnApiUrl.replace("/v2/", "") + path;
    }

    /**
     * Tính phí vận chuyển (Shipping Fee) thông qua GHN
     */
    public GhnFeeResponse calculateFee(String internalShopId, GhnFeeRequest request) {
        Shop shop = shopRepository.findById(internalShopId)
                .orElseThrow(() -> new GhnIntegrationException("Shop " + internalShopId + " not found"));

        if (shop.getGhnToken() == null || shop.getGhnShopId() == null) {
            throw new GhnIntegrationException("Shop " + internalShopId + " is not configured for GHN");
        }

        log.info("Calculating GHN shipping fee for shop: {}", internalShopId);

        GhnBaseResponse<GhnFeeResponse> response = ghnRestClient.post()
                .uri("/shipping-order/fee")
                .header("Token", shop.getGhnToken())
                .header("ShopId", String.valueOf(shop.getGhnShopId()))
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

    /**
     * Tạo đơn giao hàng (Create Shipping Order) thông qua GHN
     */
    public GhnCreateOrderResponse createOrder(String internalShopId, GhnCreateOrderRequest request) {
        Shop shop = shopRepository.findById(internalShopId)
                .orElseThrow(() -> new GhnIntegrationException("Shop " + internalShopId + " not found"));

        if (shop.getGhnToken() == null || shop.getGhnShopId() == null) {
            throw new GhnIntegrationException("Shop " + internalShopId + " is not configured for GHN");
        }

        log.info("Creating GHN shipping order for shop: {}", internalShopId);

        GhnBaseResponse<GhnCreateOrderResponse> response = ghnRestClient.post()
                .uri("/shipping-order/create")
                .header("Token", shop.getGhnToken())
                .header("ShopId", String.valueOf(shop.getGhnShopId()))
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String bodyStr = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    log.error("GHN API Error during create order. Status: {}. Body: {}", res.getStatusCode(), bodyStr);
                    throw new GhnIntegrationException("GHN API Error: " + res.getStatusCode() + " - " + bodyStr);
                })
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.getCode() != 200) {
            String msg = response != null ? response.getMessage() : "Unknown error";
            log.error("GHN API create order failed: {}", msg);
            throw new GhnIntegrationException("Failed to create GHN order: " + msg);
        }

        return response.getData();
    }

    /**
     * Lấy danh sách Tỉnh/Thành phố
     */
    public Object getProvinces() {
        if (globalGhnToken == null || globalGhnToken.isBlank()) {
            throw new GhnIntegrationException("GHN Global Token is not configured (GHN_API_TOKEN)");
        }
        String url = getMasterDataUrl("/master-data/province");
        log.info("Requesting GHN Provinces from URL: {}", url);
        return ghnRestClient.get()
                .uri(URI.create(url))
                .header("Token", globalGhnToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new GhnIntegrationException("GHN API Error: " + res.getStatusCode());
                })
                .body(Object.class);
    }

    /**
     * Lấy danh sách Quận/Huyện
     */
    public Object getDistricts(Integer provinceId) {
        if (globalGhnToken == null || globalGhnToken.isBlank()) {
            throw new GhnIntegrationException("GHN Global Token is not configured (GHN_API_TOKEN)");
        }
        String url = getMasterDataUrl("/master-data/district") + "?province_id=" + provinceId;
        return ghnRestClient.get()
                .uri(URI.create(url))
                .header("Token", globalGhnToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new GhnIntegrationException("GHN API Error: " + res.getStatusCode());
                })
                .body(Object.class);
    }

    /**
     * Lấy danh sách Phường/Xã
     */
    public Object getWards(Integer districtId) {
        if (globalGhnToken == null || globalGhnToken.isBlank()) {
            throw new GhnIntegrationException("GHN Global Token is not configured (GHN_API_TOKEN)");
        }
        String url = getMasterDataUrl("/master-data/ward") + "?district_id=" + districtId;
        return ghnRestClient.get()
                .uri(URI.create(url))
                .header("Token", globalGhnToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new GhnIntegrationException("GHN API Error: " + res.getStatusCode());
                })
                .body(Object.class);
    }
}
