package com.fashion.auth.repository;

import com.fashion.auth.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, String> {
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
