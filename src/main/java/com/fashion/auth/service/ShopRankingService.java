package com.fashion.auth.service;

import com.fashion.auth.model.Shop;
import com.fashion.auth.model.ShopRanking;
import com.fashion.auth.repository.ShopRankingRepository;
import com.fashion.auth.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ShopRankingService {

    private final ShopRepository shopRepository;
    private final ShopRankingRepository rankingRepository;

    public ShopRankingService(ShopRepository shopRepository, ShopRankingRepository rankingRepository) {
        this.shopRepository = shopRepository;
        this.rankingRepository = rankingRepository;
    }

    @Transactional
    public void calculateRankingsForCurrentMonth() {
        LocalDate period = LocalDate.now().withDayOfMonth(1);
        List<Shop> shops = shopRepository.findAll();

        for (Shop s : shops) {
            double salesScore = s.getTotalSold() * 1.0;

            double avgRatingValue = (s.getAvgRating() == null) ? 5.0 : s.getAvgRating().doubleValue();
            double ratingScore = (avgRatingValue / 5.0) * 40.0;
            double responseScore = (s.getResponseRate() == null ? 100 : s.getResponseRate()) / 100.0 * 20.0;

            // Simplified totalPoints formula (placeholder)
            int totalPoints = (int) Math.round(Math.min(100.0, salesScore / 100.0 + ratingScore + responseScore));

            ShopRanking ranking = rankingRepository.findByShopIdAndPeriodMonth(s.getId(), period).orElse(null);
            if (ranking == null) {
                ranking = ShopRanking.builder()
                        .shop(s)
                        .totalPoints(totalPoints)
                        .tier(determineTier(totalPoints))
                        .periodMonth(period)
                        .build();
            } else {
                ranking.setTotalPoints(totalPoints);
                ranking.setTier(determineTier(totalPoints));
                ranking.setPeriodMonth(period);
            }
            rankingRepository.save(ranking);
        }

        // assign rank positions
        List<ShopRanking> ordered = rankingRepository.findAll();
        ordered.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));
        int pos = 1;
        for (ShopRanking r : ordered) {
            r.setRankPosition(pos++);
            rankingRepository.save(r);
        }
    }

    private String determineTier(int points) {
        if (points >= 80) return "platinum";
        if (points >= 60) return "gold";
        if (points >= 40) return "silver";
        return "bronze";
    }
}
