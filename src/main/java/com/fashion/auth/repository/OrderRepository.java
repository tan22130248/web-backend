package com.fashion.auth.repository;

import com.fashion.auth.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId, Pageable pageable);
    Page<Order> findByShopIdOrderByCreatedAtDesc(String shopId, Pageable pageable);
    List<Order> findByBuyerIdAndStatus(String buyerId, Order.OrderStatus status);
    List<Order> findByShopIdAndStatus(String shopId, Order.OrderStatus status);
    java.util.Optional<Order> findByOrderCode(String orderCode);

    // Admin: list all orders across the marketplace
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);
    long countByStatus(Order.OrderStatus status);
    java.util.Optional<Order> findByGhnTrackingCode(String ghnTrackingCode);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.shop.id = :shopId AND o.status = 'delivered'")
    BigDecimal calculateTotalRevenue(@Param("shopId") String shopId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.shop.id = :shopId AND o.status = 'pending'")
    long countNewOrders(@Param("shopId") String shopId);

    long countByShopId(String shopId);
    long countByShopIdAndStatus(String shopId, Order.OrderStatus status);

    // native query to group revenue by date for chart (last X days or specific time range logic can be added in service)
    @Query(value = "SELECT DATE(created_at) as label, SUM(total_amount) as value FROM orders WHERE shop_id = :shopId AND status = 'delivered' AND created_at >= :startDate GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Map<String, Object>> getRevenueChartData(@Param("shopId") String shopId, @Param("startDate") java.time.LocalDateTime startDate);

    // native query to group revenue by date for specific month/year
    @Query(value = "SELECT DATE(created_at) as label, SUM(total_amount) as value " +
                   "FROM orders " +
                   "WHERE shop_id = :shopId AND status = 'delivered' " +
                   "AND MONTH(created_at) = :month AND YEAR(created_at) = :year " +
                   "GROUP BY DATE(created_at) " +
                   "ORDER BY DATE(created_at)", nativeQuery = true)
    List<Map<String, Object>> getRevenueChartDataByMonth(@Param("shopId") String shopId,
                                                         @Param("month") Integer month,
                                                         @Param("year") Integer year);

    // native query to group revenue by date for specific month/year within last X days
    @Query(value = "SELECT DATE(created_at) as label, SUM(total_amount) as value " +
                   "FROM orders " +
                   "WHERE shop_id = :shopId AND status = 'delivered' " +
                   "AND MONTH(created_at) = :month AND YEAR(created_at) = :year " +
                   "AND created_at >= :startDate " +
                   "GROUP BY DATE(created_at) " +
                   "ORDER BY DATE(created_at)", nativeQuery = true)
    List<Map<String, Object>> getRevenueChartDataByMonthAndDays(@Param("shopId") String shopId,
                                                                @Param("month") Integer month,
                                                                @Param("year") Integer year,
                                                                @Param("startDate") java.time.LocalDateTime startDate);

    // native query to group revenue by category
    @Query(value = "SELECT c.name as categoryName, SUM(oi.total_price) as revenue " +
                   "FROM order_items oi " +
                   "JOIN orders o ON oi.order_id = o.id " +
                   "JOIN products p ON oi.product_id = p.id " +
                   "JOIN categories c ON p.category_id = c.id " +
                   "WHERE o.shop_id = :shopId AND o.status = 'delivered' " +
                   "GROUP BY c.id, c.name", nativeQuery = true)
    List<Map<String, Object>> getCategoryRevenueData(@Param("shopId") String shopId);

    // native query to group revenue by category filtered by month and year
    @Query(value = "SELECT c.name as categoryName, SUM(oi.total_price) as revenue " +
                   "FROM order_items oi " +
                   "JOIN orders o ON oi.order_id = o.id " +
                   "JOIN products p ON oi.product_id = p.id " +
                   "JOIN categories c ON p.category_id = c.id " +
                   "WHERE o.shop_id = :shopId AND o.status = 'delivered' " +
                   "AND MONTH(o.created_at) = :month AND YEAR(o.created_at) = :year " +
                   "GROUP BY c.id, c.name", nativeQuery = true)
    List<Map<String, Object>> getCategoryRevenueDataByMonth(@Param("shopId") String shopId, 
                                                            @Param("month") Integer month, 
                                                            @Param("year") Integer year);
}


