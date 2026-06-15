package com.fashion.auth.service;

import com.fashion.auth.model.ShopRanking;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.ShopRankingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class ShopRankingPopulationService {

    private final ShopRepository shopRepository;
    private final ShopRankingRepository rankingRepository;
    private final Random random = new Random();

    // 50 sample shop IDs from CSV
    private static final String[] SAMPLE_SHOP_IDS = {
        "009427f1-c388-5f58-bacd-744400e79a62",
        "00f4df19-0815-5912-b11d-1d2461fadb72",
        "01043527-d9a1-5b4d-8d50-dd64d0eace95",
        "015c8460-6e18-57de-a540-94ba1e4dada3",
        "021dd5f9-77c8-5470-b3c6-f2fb36c4eb27",
        "02469d95-4929-524d-8d71-dfaacbb4656d",
        "02b46bb1-ee2e-52b4-8726-37249705399b",
        "02f15c5b-95a9-5c5b-a285-5bcd82f11969",
        "04e8cf1e-ed70-570a-a3c1-df78e15d9106",
        "04f49da1-b4cf-53ec-8271-31b466145f2d",
        "0568d87e-a86f-52dd-912c-672e420a2b9e",
        "06a9ee4b-a57c-56f2-8ad1-81e2931885d5",
        "06fb83cc-29e9-5dfb-bb13-fe9dbee821ed",
        "07255fc7-1e78-597c-92a7-6ae2fac75a6d",
        "07302abc-4016-54d1-9dcf-a5046e5db232",
        "07c06713-4fa3-5ae4-9949-33cbfd379cfd",
        "08b6be28-f2b1-577b-964d-963945c2396d",
        "08ed79e9-0d47-57e6-9fef-0c8f0a514e5b",
        "093f0f4e-866f-5fc7-9093-9ce5f1072296",
        "09900b48-cb6c-59e4-9933-0a1b2e4c4687",
        "09f2c886-89a1-5756-864f-c0286a11c75d",
        "0a73847a-75b1-50d3-a19f-c61dbce8443a",
        "0b085319-9808-5ae2-af64-d8dbbc638ff6",
        "0b226a9d-8eac-5729-a291-c84189c9fa6d",
        "0b412e56-7e58-5988-a5ea-99dcecd99ec5",
        "0efbb59c-d704-5c6a-9c59-dcef699de572",
        "0f98b36e-8f2c-5496-bc7f-b1e7832a408c",
        "0f9e15c8-6839-5add-997e-553e56e6b14e",
        "100db529-040c-56cb-9d4d-84aa8542b867",
        "10b20ca3-86e4-5f12-b7eb-8dbc5bb53125",
        "10c0a40e-8fd0-5321-a4be-23fd0f77c6d8",
        "11412f4e-2168-5639-aba0-5e1066d28492",
        "12785baa-5baa-520b-8944-e0a05dce1f96",
        "12b7587b-ad17-55ae-86e0-1da0ec50c3fa",
        "12e063c7-e7f7-5338-8f22-bbba34f3294a",
        "1304e5b3-8c6a-5b69-893f-42da6e0b23fa",
        "1361e1f3-1467-5feb-9b2e-520c8f3495d5",
        "138ddb40-158f-5d0e-82b9-81c417c94874",
        "1390a4e1-678d-572d-adcf-e4b454d125c7",
        "1474ad97-91d5-5527-bcdb-da07197c623a",
        "15082491-32bc-5b2b-9b01-5c866a5ba1c5",
        "17771e17-e9ff-58fc-bc29-ddd9ca2af367",
        "18340088-69a1-50b5-a897-a0d9986e4332",
        "184d3842-9723-593f-b2ce-67b7cba12633",
        "18eca0bf-32f2-524e-a29f-c42385670e58",
        "197ad3e9-ac5f-50d1-8783-9419008a0e51",
        "19ff1783-d2d2-5b0f-9ddb-0fa3a9ae63cd",
        "1b5bbb68-566a-52f8-ad22-217adaa65707",
        "1c7af11b-d683-55f2-8871-5988e5acdcec",
        "1cee1346-2733-5d29-97d4-3f99a3714714"
    };

    public ShopRankingPopulationService(ShopRepository shopRepository, ShopRankingRepository rankingRepository) {
        this.shopRepository = shopRepository;
        this.rankingRepository = rankingRepository;
    }

    @Transactional
    public void populateSampleRankings(int count) {
        LocalDate period = LocalDate.now().withDayOfMonth(1); // First day of current month
        List<ShopRanking> rankings = new ArrayList<>();
        
        int actualCount = Math.min(count, SAMPLE_SHOP_IDS.length);
        
        for (int i = 0; i < actualCount; i++) {
            String shopId = SAMPLE_SHOP_IDS[i];
            
            // Check if shop exists
            if (!shopRepository.existsById(shopId)) {
                System.out.println("⚠️  Shop not found: " + shopId);
                continue;
            }
            
            // Generate random points between 50-95
            int totalPoints = 50 + random.nextInt(46); // 50-95
            String tier = determineTier(totalPoints);
            
            ShopRanking ranking = ShopRanking.builder()
                    .shop(shopRepository.findById(shopId).orElse(null))
                    .totalPoints(totalPoints)
                    .tier(tier)
                    .rankPosition(i + 1)
                    .periodMonth(period)
                    .build();
            
            rankings.add(ranking);
        }
        
        rankingRepository.saveAll(rankings);
        System.out.println("✅ Populated " + rankings.size() + " shop rankings");
        System.out.println("📅 Period: " + period);
    }

    private String determineTier(int points) {
        if (points >= 80) return "platinum";
        if (points >= 60) return "gold";
        if (points >= 40) return "silver";
        return "bronze";
    }

    public long getTotalRankings() {
        return rankingRepository.count();
    }
}
