package com.fashion.auth.service;

import com.fashion.auth.dto.FeaturedShopDTO;
import com.fashion.auth.dto.NewlyVerifiedShopDTO;
import com.fashion.auth.dto.WeeklyShopDTO;
import com.fashion.auth.model.Shop;
import com.fashion.auth.model.ShopRanking;
import com.fashion.auth.repository.ShopRankingRepository;
import com.fashion.auth.repository.ShopRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopRankingRepository rankingRepository;

    public ShopService(ShopRepository shopRepository, ShopRankingRepository rankingRepository) {
        this.shopRepository = shopRepository;
        this.rankingRepository = rankingRepository;
    }

    public List<FeaturedShopDTO> getFeaturedShops() {
        // Get top 6 by totalPoints without period filter (for current display)
        List<ShopRanking> top = rankingRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()))
                .limit(6)
                .collect(Collectors.toList());
        
        return top.stream().map(r -> FeaturedShopDTO.builder()
                .id(r.getShop().getId())
                .name(r.getShop().getShopName())
                .rating(r.getShop().getAvgRating())
                .badge(r.getShop().getVerifiedAt() != null ? "Xác Nhận" : "")
                .category(null)
                .imageUrl(r.getShop().getAvatarUrl())
                .tier(r.getTier())
                .icon("🏪") // Default icon, can be customized
                .build()
        ).collect(Collectors.toList());
    }

    public WeeklyShopDTO getWeeklyShop() {
        // Get top 1 by totalPoints
        return rankingRepository.findAll().stream()
                .max((a, b) -> Integer.compare(a.getTotalPoints(), b.getTotalPoints()))
                .map(r -> new WeeklyShopDTO(
                        r.getShop().getId(),
                        r.getShop().getShopName(),
                        r.getShop().getAvgRating(),
                        "",
                        r.getShop().getAvatarUrl(),
                        r.getShop().getTotalSold(),
                        r.getShop().getResponseRate(),
                        r.getTier()
                )).orElse(null);
    }

    public List<NewlyVerifiedShopDTO> getNewlyVerifiedShops() {
        List<Shop> shops = shopRepository.findTop6ByVerifiedAtNotNullOrderByVerifiedAtDesc();
        return shops.stream().map(s -> {
            String relativeTime = getRelativeTime(s.getVerifiedAt());
            return NewlyVerifiedShopDTO.builder()
                    .id(s.getId())
                    .name(s.getShopName())
                    .imageUrl(s.getAvatarUrl())
                    .joinDate(s.getVerifiedAt() != null ? s.getVerifiedAt().toString() : null)
                    .joinDateRelative(relativeTime)
                    .build();
        }).collect(Collectors.toList());
    }

    private String getRelativeTime(LocalDate date) {
        if (date == null) return "";
        long daysAgo = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (daysAgo < 1) return "Hôm nay";
        if (daysAgo < 7) return "Tham gia " + daysAgo + " ngày trước";
        if (daysAgo < 30) return "Tham gia " + (daysAgo / 7) + " tuần trước";
        return "Tham gia " + (daysAgo / 30) + " tháng trước";
    }
}
