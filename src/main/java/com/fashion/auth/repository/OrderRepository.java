package com.fashion.auth.repository;

import com.fashion.auth.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId, Pageable pageable);
    Page<Order> findByShopIdOrderByCreatedAtDesc(String shopId, Pageable pageable);
    List<Order> findByBuyerIdAndStatus(String buyerId, Order.OrderStatus status);
    List<Order> findByShopIdAndStatus(String shopId, Order.OrderStatus status);

    // Admin: list all orders across the marketplace
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);
    long countByStatus(Order.OrderStatus status);
}


