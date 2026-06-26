package com.fashion.auth.service;

import com.fashion.auth.model.*;
import com.fashion.auth.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopPointService {

    private static final Logger log = LoggerFactory.getLogger(ShopPointService.class);

    private final ShopPointRepository shopPointRepository;
    private final PointRuleRepository pointRuleRepository;
    private final ShopRepository shopRepository;
    private final ShopRankingRepository shopRankingRepository;
    private final NotificationRepository notificationRepository;

    public ShopPointService(ShopPointRepository shopPointRepository,
                            PointRuleRepository pointRuleRepository,
                            ShopRepository shopRepository,
                            ShopRankingRepository shopRankingRepository,
                            NotificationRepository notificationRepository) {
        this.shopPointRepository = shopPointRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.shopRepository = shopRepository;
        this.shopRankingRepository = shopRankingRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Tích điểm cho shop khi đơn hàng giao thành công.
     * Idempotent: nếu đơn đã được tích điểm thì bỏ qua.
     */
    @Transactional
    public void awardPointsForDeliveredOrder(Order order) {
        // 1. Kiểm tra idempotent
        if (shopPointRepository.existsByOrderId(order.getId())) {
            log.warn("Đơn hàng {} đã được tích điểm, bỏ qua", order.getOrderCode());
            return;
        }

        // 2. Tìm PointRule phù hợp theo giá trị đơn
        PointRule rule = pointRuleRepository
                .findFirstByIsActiveTrueAndMinOrderValueLessThanEqualOrderByMinOrderValueDesc(order.getTotalAmount())
                .orElse(null);

        int basePoints = (rule != null) ? rule.getPointsPerOrder() : 10;
        BigDecimal multiplier = (rule != null) ? rule.getBonusMultiplier() : BigDecimal.ONE;
        int finalPoints = (int) Math.floor(basePoints * multiplier.doubleValue());

        // 3. Tạo bản ghi ShopPoint
        ShopPoint shopPoint = ShopPoint.builder()
                .shop(order.getShop())
                .order(order)
                .pointsEarned(finalPoints)
                .amount(order.getTotalAmount())
                .type("sale_success")
                .reason("Đơn hàng " + order.getOrderCode() + " giao thành công")
                .build();
        shopPointRepository.save(shopPoint);

        // 4. Cập nhật tổng điểm shop
        Shop shop = order.getShop();
        shop.setTotalPoints(shop.getTotalPoints() + finalPoints);
        shopRepository.save(shop);

        // 5. Cập nhật/tạo ranking tháng hiện tại
        updateMonthlyRanking(shop, finalPoints);

        // 6. Thông báo cho seller
        sendPointNotification(shop, finalPoints, order.getOrderCode());

        log.info("Tích điểm shop {}: +{} điểm (đơn {}, rule={})",
                shop.getShopName(), finalPoints, order.getOrderCode(),
                rule != null ? rule.getRuleName() : "default");
    }

    /**
     * Trừ điểm khi đơn hàng bị refund (sau delivered).
     */
    @Transactional
    public void deductPointsForRefund(Order order) {
        // Tìm bản ghi điểm đã cộng cho đơn này
        List<ShopPoint> existingPoints = shopPointRepository.findByShopIdOrderByCreatedAtDesc(order.getShop().getId());
        ShopPoint originalPoint = existingPoints.stream()
                .filter(sp -> sp.getOrder() != null && sp.getOrder().getId().equals(order.getId()))
                .findFirst()
                .orElse(null);

        if (originalPoint == null) {
            log.warn("Không tìm thấy bản ghi điểm cho đơn {} để trừ", order.getOrderCode());
            return;
        }

        int pointsToDeduct = originalPoint.getPointsEarned();

        // Tạo bản ghi trừ điểm
        ShopPoint deduction = ShopPoint.builder()
                .shop(order.getShop())
                .order(order)
                .pointsEarned(-pointsToDeduct)
                .amount(order.getTotalAmount())
                .type("refund")
                .reason("Hoàn tiền đơn hàng " + order.getOrderCode())
                .build();
        shopPointRepository.save(deduction);

        // Cập nhật tổng điểm shop
        Shop shop = order.getShop();
        shop.setTotalPoints(Math.max(0, shop.getTotalPoints() - pointsToDeduct));
        shopRepository.save(shop);

        // Cập nhật ranking
        updateMonthlyRanking(shop, -pointsToDeduct);

        log.info("Trừ điểm shop {}: -{} điểm (refund đơn {})",
                shop.getShopName(), pointsToDeduct, order.getOrderCode());
    }

    /**
     * Lấy lịch sử tích điểm của shop.
     */
    public List<ShopPoint> getPointHistory(String shopId) {
        return shopPointRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    /**
     * Lấy tổng điểm hiện tại của shop.
     */
    public int getTotalPoints(String shopId) {
        Integer total = shopPointRepository.sumPointsByShopId(shopId);
        return total != null ? total : 0;
    }

    // ── Private helpers ────────────────────────────────────────────────

    private void updateMonthlyRanking(Shop shop, int pointsDelta) {
        LocalDate period = LocalDate.now().withDayOfMonth(1);

        ShopRanking ranking = shopRankingRepository
                .findByShopIdAndPeriodMonth(shop.getId(), period)
                .orElse(null);

        if (ranking == null) {
            ranking = ShopRanking.builder()
                    .shop(shop)
                    .totalPoints(Math.max(0, pointsDelta))
                    .tier(determineTier(Math.max(0, pointsDelta)))
                    .periodMonth(period)
                    .build();
        } else {
            int newPoints = Math.max(0, ranking.getTotalPoints() + pointsDelta);
            ranking.setTotalPoints(newPoints);
            ranking.setTier(determineTier(newPoints));
        }

        shopRankingRepository.save(ranking);
    }

    private String determineTier(int points) {
        if (points >= 5000) return "diamond";
        if (points >= 2000) return "platinum";
        if (points >= 1000) return "gold";
        if (points >= 300) return "silver";
        return "bronze";
    }

    private void sendPointNotification(Shop shop, int points, String orderCode) {
        Notification notification = Notification.builder()
                .user(shop.getUser())
                .type("points")
                .title("Tích điểm thành công")
                .body("Bạn nhận được +" + points + " điểm từ đơn hàng " + orderCode)
                .build();
        notificationRepository.save(notification);
    }
}
