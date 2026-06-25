package com.fashion.auth.controller;

import com.fashion.auth.dto.OrderDto;
import com.fashion.auth.exception.AdminAccessException;
import com.fashion.auth.model.Order;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.OrderRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Admin order management — read-only oversight of every order in the marketplace.
 * Follows the manual Bearer-check pattern used by the other admin controllers.
 */
@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final OrderService orderService;

    public AdminOrderController(OrderRepository orderRepository,
                                UserRepository userRepository,
                                JwtUtils jwtUtils,
                                OrderService orderService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.orderService = orderService;
    }

    /** GET /api/admin/orders?status=&page=0&size=10 */
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            requireAdmin(token);
            Pageable pageable = PageRequest.of(page, size);

            Order.OrderStatus statusEnum = parseStatus(status);
            Page<Order> orders = statusEnum != null
                    ? orderRepository.findByStatusOrderByCreatedAtDesc(statusEnum, pageable)
                    : orderRepository.findAllByOrderByCreatedAtDesc(pageable);

            Page<OrderDto> result = orders.map(OrderDto::from);
            return ResponseEntity.ok(result);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** GET /api/admin/orders/stats — counts per status for dashboard cards */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String token) {
        try {
            requireAdmin(token);
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", orderRepository.count());
            for (Order.OrderStatus s : Order.OrderStatus.values()) {
                stats.put(s.name(), orderRepository.countByStatus(s));
            }
            return ResponseEntity.ok(stats);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** GET /api/admin/orders/{id} — full detail with items & history */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            OrderDto dto = OrderDto.from(order);
            dto.setItems(orderService.getOrderItems(id).stream()
                    .map(OrderDto.OrderItemDto::from).toList());
            dto.setHistory(orderService.getOrderHistory(id).stream()
                    .map(OrderDto.OrderHistoryDto::from).toList());

            return ResponseEntity.ok(dto);
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** DELETE /api/admin/orders/{id} — admin can delete any order */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            requireAdmin(token);
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

            // Delete related records manually (those without CASCADE)
            // 1. Delete order status history
            orderService.deleteOrderHistory(id);
            
            // 2. Delete order (will cascade delete order_items, payments, reviews with CASCADE)
            orderRepository.delete(order);
            
            return ResponseEntity.ok(message("Đã xóa đơn hàng thành công"));
        } catch (AdminAccessException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Order.OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return Order.OrderStatus.valueOf(status.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private User requireAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AdminAccessException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        String jwt = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(jwt);

        if (email == null || !jwtUtils.validateToken(jwt)) {
            throw new AdminAccessException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AdminAccessException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));

        if (!user.isActive()) {
            throw new AdminAccessException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        if (user.getRole() != User.Role.admin) {
            throw new AdminAccessException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản trị");
        }

        return user;
    }

    private static Map<String, String> message(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return body;
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(message(message));
    }
}
