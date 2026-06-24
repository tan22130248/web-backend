package com.fashion.auth.service;

import com.fashion.auth.model.*;
import com.fashion.auth.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fashion.auth.repository.ProductImageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final PaymentRepository paymentRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public record OrderItemRequest(String productId, String variantId, int quantity) {
    }

    private final GhnService ghnService;

    public OrderService(OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            PaymentRepository paymentRepository,
            ShopRepository shopRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            GhnService ghnService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.paymentRepository = paymentRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.ghnService = ghnService;
    }

    public BigDecimal calculateTotalFee(Integer toDistrictId, String toWardCode, List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<String> productIds = items.stream().map(OrderItemRequest::productId).distinct().toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<String, List<OrderItemRequest>> groupedByShop = items.stream()
                .collect(Collectors.groupingBy(item -> productMap.get(item.productId()).getShop().getId()));

        BigDecimal totalFee = BigDecimal.ZERO;

        for (var entry : groupedByShop.entrySet()) {
            String shopId = entry.getKey();
            List<OrderItemRequest> shopItems = entry.getValue();

            int totalQuantity = shopItems.stream().mapToInt(OrderItemRequest::quantity).sum();
            int weight = totalQuantity * 200; // Hardcode khối lượng mặc định 200g/sp

            com.fashion.auth.dto.ghn.GhnFeeRequest feeRequest = com.fashion.auth.dto.ghn.GhnFeeRequest.builder()
                    .toDistrictId(toDistrictId)
                    .toWardCode(toWardCode)
                    .weight(weight)
                    .insuranceValue(0)
                    .serviceTypeId(2) // Giao hàng chuẩn
                    .build();

            try {
                com.fashion.auth.dto.ghn.GhnFeeResponse feeRes = ghnService.calculateFee(shopId, feeRequest);
                if (feeRes != null && feeRes.getTotal() != null) {
                    totalFee = totalFee.add(BigDecimal.valueOf(feeRes.getTotal()));
                }
            } catch (Exception e) {
                log.warn("Failed to calculate fee for shop: {}", shopId, e);
                // Ignore or fallback to 0 fee if one shop fails
            }
        }
        return totalFee;
    }

    /**
     * Buyer đặt hàng — nhận danh sách items từ client (client-side cart).
     * Tạo 1 order cho mỗi shop (nếu items thuộc nhiều shop khác nhau).
     */
    @Transactional
    public List<Order> placeOrder(String buyerId, String shippingAddress, Integer toDistrictId, String toWardCode, String note,
            List<OrderItemRequest> items, String paymentMethod) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm không được trống");
        }

        // Load tất cả products một lần
        List<String> productIds = items.stream().map(OrderItemRequest::productId).distinct().toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Validate tất cả items trước khi tạo order
        for (OrderItemRequest item : items) {
            Product product = productMap.get(item.productId());
            if (product == null) {
                throw new RuntimeException("Sản phẩm không tồn tại: " + item.productId());
            }
            if (!product.isActive()) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không còn bán");
            }
            if (product.getStock() < item.quantity()) {
                throw new RuntimeException(
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho (còn " + product.getStock() + ")");
            }
            if (item.quantity() <= 0) {
                throw new RuntimeException("Số lượng phải lớn hơn 0");
            }
        }

        // Nhóm items theo shop
        Map<String, List<OrderItemRequest>> groupedByShop = items.stream()
                .collect(Collectors.groupingBy(item -> productMap.get(item.productId()).getShop().getId()));

        List<Order> orders = new java.util.ArrayList<>();

        for (var entry : groupedByShop.entrySet()) {
            String shopId = entry.getKey();
            List<OrderItemRequest> shopItems = entry.getValue();

            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại"));

            BigDecimal totalAmount = BigDecimal.ZERO;

            int totalQuantity = shopItems.stream().mapToInt(OrderItemRequest::quantity).sum();
            int weight = totalQuantity * 200; // Hardcode khối lượng mặc định 200g/sp

            BigDecimal shippingFee = BigDecimal.ZERO;
            com.fashion.auth.dto.ghn.GhnFeeRequest feeRequest = com.fashion.auth.dto.ghn.GhnFeeRequest.builder()
                    .toDistrictId(toDistrictId)
                    .toWardCode(toWardCode)
                    .weight(weight)
                    .insuranceValue(0)
                    .serviceTypeId(2)
                    .build();
            try {
                com.fashion.auth.dto.ghn.GhnFeeResponse feeRes = ghnService.calculateFee(shopId, feeRequest);
                if (feeRes != null && feeRes.getTotal() != null) {
                    shippingFee = BigDecimal.valueOf(feeRes.getTotal());
                }
            } catch (Exception e) {
                log.warn("Failed to calculate fee during placeOrder for shop: {}", shopId, e);
            }

            boolean isVnPay = "vnpay".equalsIgnoreCase(paymentMethod);
            Order order = Order.builder()
                    .buyer(buyer)
                    .shop(shop)
                    .orderCode("ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .status(isVnPay ? Order.OrderStatus.pending_payment : Order.OrderStatus.pending)
                    .shippingAddress(shippingAddress)
                    .toDistrictId(toDistrictId)
                    .toWardCode(toWardCode)
                    .note(note)
                    .subtotal(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .shippingFee(shippingFee)
                    .type(isVnPay ? "online" : "cod")
                    .paymentMethod(isVnPay ? "vnpay" : "cod")
                    .paymentStatus("unpaid")
                    .build();

            order = orderRepository.save(order);

            for (OrderItemRequest itemReq : shopItems) {
                Product product = productMap.get(itemReq.productId());
                BigDecimal unitPrice = product.getPrice();

                // Nếu có variant, cộng thêm price modifier
                ProductVariant variant = null;
                if (itemReq.variantId() != null && !itemReq.variantId().isBlank()) {
                    variant = productVariantRepository.findById(itemReq.variantId()).orElse(null);
                    if (variant != null && variant.getPriceModifier() != null) {
                        unitPrice = unitPrice.add(variant.getPriceModifier());
                    }
                }

                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));

                String imageUrl = null;
                if (variant != null && variant.getImageUrl() != null && !variant.getImageUrl().isBlank()) {
                    imageUrl = variant.getImageUrl();
                } else {
                    var images = productImageRepository.findByProductIdOrderBySortOrder(product.getId());
                    if (!images.isEmpty()) {
                        imageUrl = images.get(0).getImageUrl();
                    }
                }

                String snapshotJson = "{}";
                try {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", product.getId());
                    map.put("name", product.getName());
                    map.put("price", unitPrice);
                    if (imageUrl != null) {
                        map.put("imageUrl", imageUrl);
                    }
                    snapshotJson = objectMapper.writeValueAsString(map);
                } catch (Exception e) {
                    log.warn("Failed to create product snapshot", e);
                }

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .variant(variant)
                        .productSnapshot(snapshotJson)
                        .quantity(itemReq.quantity())
                        .unitPrice(unitPrice)
                        .totalPrice(itemTotal)
                        .build();

                orderItemRepository.save(orderItem);
                totalAmount = totalAmount.add(itemTotal);

                // Giảm tồn kho
                product.setStock(product.getStock() - itemReq.quantity());
                productRepository.save(product);
            }

            order.setSubtotal(totalAmount);
            order.setTotalAmount(totalAmount.add(order.getShippingFee()));
            orderRepository.save(order);

            // Tạo payment record
            Payment payment = Payment.builder()
                    .order(order)
                    .method(isVnPay ? "vnpay" : "cod")
                    .amount(order.getTotalAmount())
                    .status(Payment.PaymentStatus.pending)
                    .build();
            paymentRepository.save(payment);

            // Ghi lịch sử trạng thái
            String initialStatus = isVnPay ? "pending_payment" : "pending";
            saveStatusHistory(order, null, initialStatus, "system",
                    isVnPay ? "Đơn hàng chờ thanh toán VNPay" : "Đơn hàng được tạo");

            // Chỉ thông báo seller ngay nếu COD (VNPay thông báo sau khi IPN confirm)
            if (!isVnPay) {
                sendNotification(shop.getUser().getId(), "order",
                        "Đơn hàng mới", "Bạn có đơn hàng mới cần xác nhận");
            }

            orders.add(order);
            log.info("Order created: id={}, shop={}, total={}", order.getId(), shopId, totalAmount);
        }

        return orders;
    }

    /** Seller xác nhận đơn hàng */
    @Transactional
    public Order confirmOrder(String sellerId, String orderId) {
        Order order = getOrderForSeller(sellerId, orderId);
        validateStatusTransition(order, Order.OrderStatus.confirmed);

        order.setStatus(Order.OrderStatus.confirmed);
        orderRepository.save(order);

        saveStatusHistory(order, "pending", "confirmed", "seller", "Seller xác nhận đơn hàng");
        sendNotification(order.getBuyer().getId(), "order",
                "Đơn hàng đã xác nhận", "Đơn hàng của bạn đã được xác nhận bởi người bán");

        return order;
    }

    /** Seller đánh dấu đã giao cho vận chuyển */
    @Transactional
    public Order shipOrder(String sellerId, String orderId) {
        Order order = getOrderForSeller(sellerId, orderId);
        validateStatusTransition(order, Order.OrderStatus.shipping);

        try {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            
            List<com.fashion.auth.dto.ghn.GhnCreateOrderRequest.Item> ghnItems = orderItems.stream().map(oi -> 
                com.fashion.auth.dto.ghn.GhnCreateOrderRequest.Item.builder()
                    .name(oi.getProduct().getName())
                    .code(oi.getProduct().getId())
                    .quantity(oi.getQuantity())
                    .price(oi.getUnitPrice().intValue())
                    .weight(200) // Hardcoded 200g
                    .build()
            ).toList();

            int totalWeight = ghnItems.stream().mapToInt(i -> i.getWeight() * i.getQuantity()).sum();
            int codAmount = "cod".equals(order.getType()) ? order.getTotalAmount().intValue() : 0;

            com.fashion.auth.dto.ghn.GhnCreateOrderRequest ghnRequest = com.fashion.auth.dto.ghn.GhnCreateOrderRequest.builder()
                .paymentTypeId(1) // 1: Shop trả cước
                .note(order.getNote() != null ? order.getNote() : "")
                .requiredNote("CHOXEMHANGKHONGTHU")
                .clientOrderCode(order.getOrderCode())
                .toName(order.getBuyer().getFullName() != null && !order.getBuyer().getFullName().isBlank() ? order.getBuyer().getFullName() : "Customer")
                .toPhone(order.getBuyer().getPhone() != null && !order.getBuyer().getPhone().isBlank() ? order.getBuyer().getPhone() : "0909090909")
                .toAddress(order.getShippingAddress())
                .toWardCode(order.getToWardCode())
                .toDistrictId(order.getToDistrictId())
                .codAmount(codAmount)
                .content("Order " + order.getOrderCode())
                .weight(totalWeight)
                .insuranceValue(order.getSubtotal().intValue())
                .serviceTypeId(2)
                .items(ghnItems)
                .build();

            com.fashion.auth.dto.ghn.GhnCreateOrderResponse ghnResponse = ghnService.createOrder(order.getShop().getId(), ghnRequest);
            if (ghnResponse != null && ghnResponse.getOrderCode() != null) {
                order.setGhnTrackingCode(ghnResponse.getOrderCode());
            }
        } catch (Exception e) {
            log.error("Failed to create GHN order for shop {}", order.getShop().getId(), e);
            throw new RuntimeException("Lỗi khi tạo vận đơn GHN: " + e.getMessage());
        }

        order.setStatus(Order.OrderStatus.shipping);
        orderRepository.save(order);

        saveStatusHistory(order, "confirmed", "shipping", "seller", "Đơn hàng đang được giao");
        sendNotification(order.getBuyer().getId(), "order",
                "Đơn hàng đang giao", "Đơn hàng của bạn đang được vận chuyển");

        return order;
    }

    /** Đánh dấu đã giao thành công (demo: seller tự bấm) */
    @Transactional
    public Order deliverOrder(String sellerId, String orderId) {
        Order order = getOrderForSeller(sellerId, orderId);
        validateStatusTransition(order, Order.OrderStatus.delivered);

        order.setStatus(Order.OrderStatus.delivered);
        orderRepository.save(order);

        // Cập nhật payment
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(Payment.PaymentStatus.paid);
            payment.setPaidAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);
        });

        // Cập nhật sold_count cho sản phẩm
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setSoldCount(product.getSoldCount() + item.getQuantity());
            productRepository.save(product);
        }

        saveStatusHistory(order, "shipping", "delivered", "seller", "Đơn hàng đã giao thành công");
        sendNotification(order.getBuyer().getId(), "order",
                "Đã giao hàng", "Đơn hàng của bạn đã được giao thành công");

        return order;
    }

    /** Buyer hoặc Seller huỷ đơn */
    @Transactional
    public Order cancelOrder(String userId, String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Xác định ai đang huỷ
        String cancelledBy;
        if (order.getBuyer().getId().equals(userId)) {
            cancelledBy = "buyer";
        } else {
            Shop shop = shopRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không có quyền huỷ đơn hàng này"));
            if (!order.getShop().getId().equals(shop.getId())) {
                throw new RuntimeException("Không có quyền huỷ đơn hàng này");
            }
            cancelledBy = "seller";
        }

        // Chỉ huỷ được khi pending hoặc confirmed
        if (order.getStatus() != Order.OrderStatus.pending
                && order.getStatus() != Order.OrderStatus.confirmed) {
            throw new RuntimeException("Không thể huỷ đơn hàng ở trạng thái hiện tại");
        }

        String oldStatus = order.getStatus().name();
        order.setStatus(Order.OrderStatus.cancelled);
        orderRepository.save(order);

        // Hoàn lại tồn kho
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        saveStatusHistory(order, oldStatus, "cancelled", cancelledBy,
                reason != null ? reason : "Đơn hàng bị huỷ");

        // Thông báo cho bên còn lại
        String notifyUserId = cancelledBy.equals("buyer")
                ? order.getShop().getUser().getId()
                : order.getBuyer().getId();
        sendNotification(notifyUserId, "order",
                "Đơn hàng đã huỷ", "Đơn hàng đã bị huỷ bởi " + cancelledBy);

        return order;
    }

    /** Buyer yêu cầu hoàn tiền (sau khi delivered) */
    @Transactional
    public Order requestRefund(String buyerId, String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!order.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("Không có quyền yêu cầu hoàn tiền cho đơn hàng này");
        }

        if (order.getStatus() != Order.OrderStatus.delivered) {
            throw new RuntimeException("Chỉ có thể yêu cầu hoàn tiền sau khi đã nhận hàng");
        }

        order.setStatus(Order.OrderStatus.refunded);
        orderRepository.save(order);

        // Cập nhật payment
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(Payment.PaymentStatus.refunded);
            paymentRepository.save(payment);
        });

        saveStatusHistory(order, "delivered", "refunded", "buyer",
                reason != null ? reason : "Buyer yêu cầu hoàn tiền");

        sendNotification(order.getShop().getUser().getId(), "order",
                "Yêu cầu hoàn tiền", "Buyer yêu cầu hoàn tiền cho đơn hàng");

        return order;
    }

    /**
     * Xác nhận thanh toán VNPay từ IPN callback.
     * Idempotent: kiểm tra paymentStatus trước khi update.
     */
    @Transactional
    public void confirmVnPayPayment(String orderCode, String vnpTransactionNo, boolean isSuccess) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại: " + orderCode));

        Payment payment = paymentRepository.findByOrderOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment cho đơn: " + orderCode));

        if (isSuccess) {
            order.setStatus(Order.OrderStatus.pending);
            order.setPaymentStatus("paid");
            orderRepository.save(order);

            payment.setStatus(Payment.PaymentStatus.paid);
            payment.setTransactionCode(vnpTransactionNo);
            payment.setPaidAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            saveStatusHistory(order, "pending_payment", "pending", "system",
                    "Thanh toán VNPay thành công - Mã GD: " + vnpTransactionNo);

            // Thông báo seller sau khi thanh toán thành công
            sendNotification(order.getShop().getUser().getId(), "order",
                    "Đơn hàng mới", "Bạn có đơn hàng mới đã thanh toán VNPay, cần xác nhận");

            log.info("VNPay payment confirmed: orderCode={}, txn={}", orderCode, vnpTransactionNo);
        } else {
            order.setPaymentStatus("failed");
            orderRepository.save(order);

            payment.setStatus(Payment.PaymentStatus.failed);
            paymentRepository.save(payment);

            log.info("VNPay payment failed: orderCode={}, txn={}", orderCode, vnpTransactionNo);
        }
    }

    // ── Query methods ──────────────────────────────────────────────────

    public Order getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại: " + orderCode));
    }

    public Page<Order> getBuyerOrders(String buyerId, Pageable pageable) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
    }

    public Page<Order> getShopOrders(String userId, Pageable pageable) {
        Shop shop = shopRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId(), pageable);
    }

    public Page<Order> getShopOrdersFiltered(String userId, String status, String fromDate, String toDate, String paymentMethod, String paymentStatus, String search, Pageable pageable) {
        Shop shop = shopRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));

        org.springframework.data.jpa.domain.Specification<Order> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("shop").get("id"), shop.getId()));

            if (status != null && !status.isBlank()) {
                try {
                    Order.OrderStatus os = Order.OrderStatus.valueOf(status.toLowerCase());
                    predicates.add(cb.equal(root.get("status"), os));
                } catch (IllegalArgumentException e) {
                    // ignore invalid status
                }
            }
            if (paymentMethod != null && !paymentMethod.isBlank()) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }
            if (paymentStatus != null && !paymentStatus.isBlank()) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (fromDate != null && !fromDate.isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), java.time.LocalDateTime.parse(fromDate + "T00:00:00")));
            }
            if (toDate != null && !toDate.isBlank()) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), java.time.LocalDateTime.parse(toDate + "T23:59:59")));
            }
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderCode")), keyword),
                        cb.like(cb.lower(root.get("buyer").get("fullName")), keyword),
                        cb.like(cb.lower(root.get("shippingAddress")), keyword)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable);
    }

    public Order getOrderDetail(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        boolean isBuyer = order.getBuyer().getId().equals(userId);
        boolean isSeller = shopRepository.findByUserId(userId)
                .map(shop -> shop.getId().equals(order.getShop().getId()))
                .orElse(false);

        if (!isBuyer && !isSeller) {
            throw new RuntimeException("Không có quyền xem đơn hàng này");
        }

        return order;
    }

    public List<OrderItem> getOrderItems(String orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public List<OrderStatusHistory> getOrderHistory(String orderId) {
        return statusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private Order getOrderForSeller(String sellerId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        Shop shop = shopRepository.findByUserId(sellerId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));

        if (!order.getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Không có quyền thao tác đơn hàng này");
        }

        return order;
    }

    private void validateStatusTransition(Order order, Order.OrderStatus target) {
        Order.OrderStatus current = order.getStatus();
        boolean valid = switch (target) {
            case confirmed -> current == Order.OrderStatus.pending;
            case shipping -> current == Order.OrderStatus.confirmed;
            case delivered -> current == Order.OrderStatus.shipping;
            case cancelled -> current == Order.OrderStatus.pending || current == Order.OrderStatus.confirmed;
            case refunded -> current == Order.OrderStatus.delivered;
            default -> false;
        };

        if (!valid) {
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + current + "' sang '" + target + "'");
        }
    }

    private void saveStatusHistory(Order order, String oldStatus, String newStatus,
            String changedBy, String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .note(note)
                .build();
        statusHistoryRepository.save(history);
    }

    private void sendNotification(String userId, String type, String title, String body) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return;

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .build();
        notificationRepository.save(notification);
    }
}
