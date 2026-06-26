package com.fashion.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    public void testGetCategoryRevenueDataByMonthExists() {
        // This test verifies that the method exists and is callable
        // It will fail to compile if the method doesn't exist
        List<Map<String, Object>> result = orderRepository.getCategoryRevenueDataByMonth("test-shop-id", 6, 2026);
        assertNotNull(result);
    }
}
