package com.fashion.auth.repository;

import com.fashion.auth.model.PointRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PointRuleRepository extends JpaRepository<PointRule, String> {
    /**
     * Tìm rule active phù hợp nhất theo giá trị đơn hàng.
     * Ưu tiên rule có min_order_value lớn nhất mà <= totalAmount.
     */
    Optional<PointRule> findFirstByIsActiveTrueAndMinOrderValueLessThanEqualOrderByMinOrderValueDesc(BigDecimal totalAmount);
}
