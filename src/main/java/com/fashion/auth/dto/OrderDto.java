package com.fashion.auth.dto;

import com.fashion.auth.model.Order;
import com.fashion.auth.model.OrderItem;
import com.fashion.auth.model.OrderStatusHistory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private String id;
    private String orderCode;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private String shippingAddress;
    private String note;
    private String type;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentUrl;  // chỉ có giá trị khi paymentMethod=vnpay
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Buyer info
    private String buyerId;
    private String buyerName;
    private String buyerEmail;

    // Shop info
    private String shopId;
    private String shopName;
    private String shopAvatarUrl;

    // Optional: items & history (populated on detail view)
    private List<OrderItemDto> items;
    private List<OrderHistoryDto> history;

    public static OrderDto from(Order o) {
        OrderDto dto = new OrderDto();
        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());
        dto.setStatus(o.getStatus().name());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setSubtotal(o.getSubtotal());
        dto.setShippingFee(o.getShippingFee());
        dto.setShippingAddress(o.getShippingAddress());
        dto.setNote(o.getNote());
        dto.setType(o.getType());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());

        if (o.getBuyer() != null) {
            dto.setBuyerId(o.getBuyer().getId());
            dto.setBuyerName(o.getBuyer().getFullName());
            dto.setBuyerEmail(o.getBuyer().getEmail());
        }
        if (o.getShop() != null) {
            dto.setShopId(o.getShop().getId());
            dto.setShopName(o.getShop().getShopName());
            dto.setShopAvatarUrl(o.getShop().getAvatarUrl());
        }
        return dto;
    }

    @Data
    public static class OrderItemDto {
        private String id;
        private String productId;
        private String productName;
        private String productImageUrl;
        private String variantId;
        private String variantSize;
        private String variantColor;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private boolean reviewed;

        public static OrderItemDto from(OrderItem item) {
            OrderItemDto dto = new OrderItemDto();
            dto.setId(item.getId());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setTotalPrice(item.getTotalPrice());

            if (item.getProduct() != null) {
                dto.setProductId(item.getProduct().getId());
                dto.setProductName(item.getProduct().getName());
            }
            if (item.getVariant() != null) {
                dto.setVariantId(item.getVariant().getId());
                dto.setVariantSize(item.getVariant().getSize());
                dto.setVariantColor(item.getVariant().getColor());
            }

            // Extract productImageUrl from productSnapshot if available
            if (item.getProductSnapshot() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(item.getProductSnapshot());
                    if (node.has("imageUrl") && !node.get("imageUrl").isNull()) {
                        dto.setProductImageUrl(node.get("imageUrl").asText());
                    }
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }

            return dto;
        }
    }

    @Data
    public static class OrderHistoryDto {
        private String id;
        private String oldStatus;
        private String newStatus;
        private String changedBy;
        private String note;
        private LocalDateTime createdAt;

        public static OrderHistoryDto from(OrderStatusHistory h) {
            OrderHistoryDto dto = new OrderHistoryDto();
            dto.setId(h.getId());
            dto.setOldStatus(h.getOldStatus());
            dto.setNewStatus(h.getNewStatus());
            dto.setChangedBy(h.getChangedBy());
            dto.setNote(h.getNote());
            dto.setCreatedAt(h.getCreatedAt());
            return dto;
        }
    }
}
