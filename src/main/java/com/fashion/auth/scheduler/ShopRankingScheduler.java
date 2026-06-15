package com.fashion.auth.scheduler;

import com.fashion.auth.service.ShopRankingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShopRankingScheduler {

    private final ShopRankingService rankingService;

    public ShopRankingScheduler(ShopRankingService rankingService) {
        this.rankingService = rankingService;
    }

    // Run daily at 00:00 server time
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyRanking() {
        rankingService.calculateRankingsForCurrentMonth();
    }
}
