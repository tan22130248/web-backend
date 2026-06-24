package com.fashion.auth.repository;

import com.fashion.auth.model.ShopPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopPointRepository extends JpaRepository<ShopPoint, String> {
    @Query("SELECT SUM(sp.pointsEarned) FROM ShopPoint sp WHERE sp.shop.id = :shopId")
    Integer sumPointsByShopId(@Param("shopId") String shopId);
}
