package com.fashion.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class VnPayService {

    @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${app.vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${app.vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${app.vnpay.return-url:http://localhost:3000/payment/result}")
    private String returnUrl;

    @Value("${app.vnpay.version:2.1.0}")
    private String version;

    @Value("${app.vnpay.command:pay}")
    private String command;

    @Value("${app.vnpay.order-type:other}")
    private String orderType;

    /**
     * Tạo URL thanh toán VNPay.
     *
     * @param orderCode  Mã đơn hàng (vnp_TxnRef) — phải duy nhất trong ngày
     * @param amountVnd  Số tiền VND (sẽ nhân 100 trước khi gửi)
     * @param orderInfo  Mô tả đơn hàng — KHÔNG dấu, KHÔNG ký tự đặc biệt
     * @param clientIp   IP của client
     * @return URL redirect sang trang thanh toán VNPay
     */
    public String createPaymentUrl(String orderCode, long amountVnd,
                                   String orderInfo, String clientIp) {
        TimeZone vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(vnTimeZone);

        // formatter phải được set timezone tường minh
        // nếu không nó sẽ dùng JVM default timezone (có thể là UTC)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(vnTimeZone);

        String createDate = formatter.format(cld.getTime());
        cld.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(cld.getTime());

        log.info("VNPay createPaymentUrl: orderCode={}, createDate={}, expireDate={}",
                orderCode, createDate, expireDate);
        // Dùng TreeMap để tự sort key alphabet — bắt buộc theo doc VNPay
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", version);
        vnpParams.put("vnp_Command", command);
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amountVnd * 100));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderCode);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", orderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", clientIp);
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        // Build hashData và query string đồng thời
        // hashData dùng URLEncoder chuẩn (spaces → +) để khớp với thuật toán VNPay
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = vnpParams.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                // hashData: encode chuẩn, spaces → + (giống VNPay PHP urlencode)
                hashData.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                // query: URL dùng %20 cho đẹp, nhưng không ảnh hưởng verify
                query.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
                if (itr.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        // Ký HMAC-SHA512 rồi append vào cuối URL
        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        String fullUrl = payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
        log.info("VNPay payment URL created for order: {}", orderCode);
        return fullUrl;
    }

    /**
     * Verify chữ ký từ VNPay callback (IPN hoặc ReturnURL).
     * Nhận raw params từ HttpServletRequest.getParameterMap().
     *
     * @param rawParams HttpServletRequest.getParameterMap()
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String[]> rawParams) {
        String receivedHash = getFirstParam(rawParams, "vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            log.warn("VNPay callback missing vnp_SecureHash");
            return false;
        }

        // Tái tạo hashData — bỏ vnp_SecureHash và vnp_SecureHashType, sort alphabet
        // QUAN TRỌNG: dùng raw value (không encode lại) vì servlet đã tự URL-decode
        Map<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : rawParams.entrySet()) {
            String key = entry.getKey();
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) continue;
            String value = (entry.getValue() != null && entry.getValue().length > 0)
                    ? entry.getValue()[0] : "";
            if (!value.isEmpty()) {
                fields.put(key, value);
            }
        }

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = fields.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            // Encode lại giống VNPay (spaces → +) để hash khớp
            hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            if (itr.hasNext()) hashData.append('&');
        }

        log.info("VNPay verifySignature hashData: {}", hashData);
        String computedHash = hmacSHA512(hashSecret, hashData.toString());
        boolean valid = computedHash.equalsIgnoreCase(receivedHash);
        if (!valid) {
            log.warn("VNPay signature mismatch.\n  computed={}\n  received={}", computedHash, receivedHash);
        }
        return valid;
    }

    /** Lấy giá trị đầu tiên từ raw param map */
    public String getFirstParam(Map<String, String[]> rawParams, String key) {
        String[] values = rawParams.get(key);
        return (values != null && values.length > 0) ? values[0] : null;
    }

    /** HMAC-SHA512 — thuật toán duy nhất VNPay v2.1.0 hỗ trợ */
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA512", e);
            throw new RuntimeException("Lỗi khi tạo chữ ký VNPay", e);
        }
    }
}
