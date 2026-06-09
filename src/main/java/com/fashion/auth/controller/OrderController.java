package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.OrderDto;
import com.fashion.auth.model.Order;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OrderController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, JwtUtils jwtUtils, UserRepository userRepository) {
        this.orderService = orderService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/orders/calculate-fee
     * Body: { toDistrictId, toWardCode, items: [{productId, variantId?, quantity}] }
     */
    @PostMapping("/calculate-fee")
    public ResponseEntity<?> calculateFee(
            @RequestBody Map<String, Object> body) {
        try {
            Integer toDistrictId = (Integer) body.get("toDistrictId");
            String toWardCode = (String) body.get("toWardCode");

            if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Vui lòng nhập districtId và wardCode"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
            if (rawItems == null || rawItems.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Danh sách sản phẩm không được trống"));
            }

            List<OrderService.OrderItemRequest> items = rawItems.stream()
                    .map(m -> new OrderService.OrderItemRequest(
                            (String) m.get("productId"),
                            (String) m.get("variantId"),
                            m.containsKey("quantity") ? ((Number) m.get("quantity")).intValue() : 1
                    ))
                    .toList();

            java.math.BigDecimal totalFee = orderService.calculateTotalFee(toDistrictId, toWardCode, items);
            return ResponseEntity.ok(Map.of("totalFee", totalFee));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/orders
     * Body: { shippingAddress, toDistrictId, toWardCode, note?, items: [{productId, variantId?, quantity}] }
     */
    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            String userId = getUserId(token);
            String shippingAddress = (String) body.get("shippingAddress");
            Integer toDistrictId = (Integer) body.get("toDistrictId");
            String toWardCode = (String) body.get("toWardCode");
            String note = (String) body.get("note");

            if (shippingAddress == null || shippingAddress.isBlank()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Vui lòng nhập địa chỉ giao hàng"));
            }
            if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Vui lòng nhập districtId và wardCode"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
            if (rawItems == null || rawItems.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Danh sách sản phẩm không được trống"));
            }

            List<OrderService.OrderItemRequest> items = rawItems.stream()
                    .map(m -> new OrderService.OrderItemRequest(
                            (String) m.get("productId"),
                            (String) m.get("variantId"),
                            m.containsKey("quantity") ? ((Number) m.get("quantity")).intValue() : 1
                    ))
                    .toList();

            List<OrderDto> result = orderService.placeOrder(userId, shippingAddress, toDistrictId, toWardCode, note, items)
                    .stream().map(OrderDto::from).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/orders?role=buyer|seller&page=0&size=10 */
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "buyer") String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String userId = getUserId(token);
            Pageable pageable = PageRequest.of(page, size);

            Page<Order> orders = "seller".equals(role)
                    ? orderService.getShopOrdersFiltered(userId, status, fromDate, toDate, paymentMethod, paymentStatus, search, pageable)
                    : orderService.getBuyerOrders(userId, pageable);

            Page<OrderDto> result = orders.map(OrderDto::from);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/orders/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            String userId = getUserId(token);
            Order order = orderService.getOrderDetail(userId, id);

            OrderDto dto = OrderDto.from(order);
            dto.setItems(orderService.getOrderItems(id).stream()
                    .map(OrderDto.OrderItemDto::from).toList());
            dto.setHistory(orderService.getOrderHistory(id).stream()
                    .map(OrderDto.OrderHistoryDto::from).toList());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/orders/{id}/confirm — Seller xác nhận */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<?> confirmOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            String userId = getUserId(token);
            return ResponseEntity.ok(OrderDto.from(orderService.confirmOrder(userId, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/orders/{id}/ship — Seller giao hàng */
    @PatchMapping("/{id}/ship")
    public ResponseEntity<?> shipOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            String userId = getUserId(token);
            return ResponseEntity.ok(OrderDto.from(orderService.shipOrder(userId, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/orders/{id}/deliver — Đánh dấu đã giao */
    @PatchMapping("/{id}/deliver")
    public ResponseEntity<?> deliverOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            String userId = getUserId(token);
            return ResponseEntity.ok(OrderDto.from(orderService.deliverOrder(userId, id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/orders/{id}/cancel { reason? } */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String userId = getUserId(token);
            String reason = body != null ? body.get("reason") : null;
            return ResponseEntity.ok(OrderDto.from(orderService.cancelOrder(userId, id, reason)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PATCH /api/orders/{id}/refund { reason? } */
    @PatchMapping("/{id}/refund")
    public ResponseEntity<?> requestRefund(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String userId = getUserId(token);
            String reason = body != null ? body.get("reason") : null;
            return ResponseEntity.ok(OrderDto.from(orderService.requestRefund(userId, id, reason)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private String getUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ");
        }
        String jwt = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(jwt);
        if (email == null || !jwtUtils.validateToken(jwt)) {
            throw new RuntimeException("Token không hợp lệ");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return user.getId();
    }
}
