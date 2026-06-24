package com.fashion.auth.controller;

import com.fashion.auth.service.OrderService;
import com.fashion.auth.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PaymentController xử lý các endpoint liên quan đến VNPay:
 * - POST /api/payments/vnpay/create : tạo URL thanh toán cho order đang pending_payment
 * - GET  /api/payments/vnpay/ipn    : IPN callback từ VNPay server (PUBLIC, update DB)
 * - GET  /api/payments/vnpay/return : ReturnURL trình duyệt redirect (PUBLIC, chỉ redirect)
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class PaymentController {

    private final VnPayService vnPayService;
    private final OrderService orderService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // ──────────────────────────────────────────────────────────────────
    // POST /api/payments/vnpay/create
    // Body: { "orderCode": "ORD-XXXXXXXX" }
    // Auth: Bearer JWT (user tự gọi nếu muốn lấy lại URL)
    // ──────────────────────────────────────────────────────────────────
    @PostMapping("/vnpay/create")
    public ResponseEntity<?> createPaymentUrl(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String orderCode = (String) body.get("orderCode");
            if (orderCode == null || orderCode.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Thiếu orderCode"));
            }

            // Lấy thông tin đơn hàng để lấy số tiền
            com.fashion.auth.model.Order order = orderService.getOrderByCode(orderCode);
            BigDecimal amount = order.getTotalAmount();

            String clientIp = getClientIp(request);
            String paymentUrl = vnPayService.createPaymentUrl(
                    orderCode,
                    amount.longValue(),
                    "Thanh toan don hang " + orderCode,
                    clientIp);

            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/payments/vnpay/ipn
    // Server-to-server từ VNPay — KHÔNG cần JWT, phải trả JSON RspCode
    // ──────────────────────────────────────────────────────────────────
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> handleIpn(HttpServletRequest request) {
        Map<String, String[]> rawParams = request.getParameterMap();

        // Bước 1: Verify checksum
        if (!vnPayService.verifySignature(rawParams)) {
            log.warn("VNPay IPN invalid checksum. Params: {}", rawParams.keySet());
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        String orderCode      = vnPayService.getFirstParam(rawParams, "vnp_TxnRef");
        String vnpAmount      = vnPayService.getFirstParam(rawParams, "vnp_Amount");
        String responseCode   = vnPayService.getFirstParam(rawParams, "vnp_ResponseCode");
        String transactionStatus = vnPayService.getFirstParam(rawParams, "vnp_TransactionStatus");
        String transactionNo  = vnPayService.getFirstParam(rawParams, "vnp_TransactionNo");

        try {
            // Bước 2: Tìm order
            com.fashion.auth.model.Order order = orderService.getOrderByCode(orderCode);

            // Bước 3: Kiểm tra số tiền (vnp_Amount đã nhân 100)
            long receivedAmount = Long.parseLong(vnpAmount) / 100;
            if (receivedAmount != order.getTotalAmount().longValue()) {
                log.warn("VNPay IPN amount mismatch. expected={}, received={}",
                        order.getTotalAmount(), receivedAmount);
                return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
            }

            // Bước 4: Idempotent — kiểm tra đã xử lý chưa
            if (!"unpaid".equals(order.getPaymentStatus())) {
                log.info("VNPay IPN already processed for order: {}", orderCode);
                return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
            }

            // Bước 5: Xử lý kết quả thanh toán
            boolean isSuccess = "00".equals(responseCode) && "00".equals(transactionStatus);
            orderService.confirmVnPayPayment(orderCode, transactionNo, isSuccess);

            log.info("VNPay IPN processed: orderCode={}, success={}, txn={}", orderCode, isSuccess, transactionNo);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));

        } catch (Exception e) {
            // Nếu không tìm thấy đơn hàng
            if (e.getMessage() != null && e.getMessage().contains("không tồn tại")) {
                log.warn("VNPay IPN order not found: {}", orderCode);
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not Found"));
            }
            log.error("VNPay IPN unexpected error for order: {}", orderCode, e);
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknown error"));
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/payments/vnpay/return
    // Browser redirect từ VNPay — KHÔNG update DB, chỉ redirect frontend
    // ──────────────────────────────────────────────────────────────────
    @GetMapping("/vnpay/return")
    public void handleReturn(HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response) throws Exception {
        Map<String, String[]> rawParams = request.getParameterMap();
        String orderCode    = vnPayService.getFirstParam(rawParams, "vnp_TxnRef");
        String responseCode = vnPayService.getFirstParam(rawParams, "vnp_ResponseCode");

        boolean validSignature = vnPayService.verifySignature(rawParams);
        boolean isSuccess = validSignature && "00".equals(responseCode);

        String redirectUrl = frontendUrl + "/payment/result?status="
                + (isSuccess ? "success" : "fail")
                + "&orderCode=" + (orderCode != null ? orderCode : "");

        log.info("VNPay return: orderCode={}, responseCode={}, redirectTo={}", orderCode, responseCode, redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return (ip != null && !ip.isBlank()) ? ip : "127.0.0.1";
    }
}
