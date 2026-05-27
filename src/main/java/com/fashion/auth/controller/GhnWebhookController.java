package com.fashion.auth.controller;

import com.fashion.auth.model.Order;
import com.fashion.auth.repository.OrderRepository;
import com.fashion.auth.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook/ghn")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class GhnWebhookController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> handleGhnWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received GHN Webhook: {}", payload);

        try {
            String ghnOrderCode = (String) payload.get("OrderCode");
            String status = (String) payload.get("Status");

            if (ghnOrderCode == null || status == null) {
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            Order order = orderRepository.findByGhnTrackingCode(ghnOrderCode).orElse(null);
            if (order == null) {
                log.warn("Order with GHN code {} not found in system", ghnOrderCode);
                return ResponseEntity.ok("Ignored");
            }

            switch (status) {
                case "delivered":
                    if (order.getStatus() == Order.OrderStatus.shipping) {
                        orderService.deliverOrder(order.getShop().getUser().getId(), order.getId());
                        log.info("Auto-delivered order {} via webhook", order.getId());
                    }
                    break;
                case "cancel":
                case "return":
                case "returned":
                    if (order.getStatus() == Order.OrderStatus.shipping || order.getStatus() == Order.OrderStatus.pending || order.getStatus() == Order.OrderStatus.confirmed) {
                        orderService.cancelOrder(order.getShop().getUser().getId(), order.getId(), "GHN status: " + status);
                        log.info("Auto-cancelled order {} via webhook", order.getId());
                    }
                    break;
                default:
                    log.info("GHN Webhook status '{}' ignored for order {}", status, order.getId());
                    break;
            }

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            log.error("Error processing GHN webhook", e);
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }
}
