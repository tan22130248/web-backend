package com.fashion.auth.repository;

import com.fashion.auth.model.ShopRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRankingRepository extends JpaRepository<ShopRanking, String> {
    List<ShopRanking> findTop6ByPeriodMonthOrderByTotalPointsDesc(LocalDate periodMonth);
    Optional<ShopRanking> findTopByPeriodMonthOrderByTotalPointsDesc(LocalDate periodMonth);
    Optional<ShopRanking> findByShopIdAndPeriodMonth(String shopId, LocalDate periodMonth);
}
