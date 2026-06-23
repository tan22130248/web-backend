package com.fashion.auth.scheduler;

import com.fashion.auth.repository.ShopRankingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyShopScheduler {

    private final ShopRankingRepository rankingRepository;

    public WeeklyShopScheduler(ShopRankingRepository rankingRepository) {
        this.rankingRepository = rankingRepository;
    }

    // Run every Monday at 00:00
    @Scheduled(cron = "0 0 0 * * MON")
    public void weeklyFeature() {
        // Currently nothing complex: top ranked shop for current month will be used by API
        // We can add caching/marking logic here later
        rankingRepository.findAll();
    }
}
