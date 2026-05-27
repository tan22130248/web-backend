package com.fashion.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.auth.dto.ghn.GhnShopConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnShopConfigStore {

    private static final String STORE_FILE = "ghn_shop_configs.json";
    private final ObjectMapper objectMapper;
    private final Map<String, GhnShopConfig> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadFromFile();
    }

    public Optional<GhnShopConfig> getConfig(String shopId) {
        return Optional.ofNullable(store.get(shopId));
    }

    public void saveConfig(String shopId, String token, Integer ghnShopId) {
        GhnShopConfig config = GhnShopConfig.builder()
                .ghnToken(token)
                .ghnShopId(ghnShopId)
                .build();
        store.put(shopId, config);
        saveToFile();
        log.info("Saved GHN config for shop: {}", shopId);
    }

    private void loadFromFile() {
        File file = new File(STORE_FILE);
        if (file.exists()) {
            try {
                Map<String, GhnShopConfig> data = objectMapper.readValue(file, new TypeReference<>() {});
                store.putAll(data);
                log.info("Loaded {} GHN shop configs from file.", data.size());
            } catch (IOException e) {
                log.error("Failed to load GHN shop configs from file.", e);
            }
        }
    }

    private void saveToFile() {
        File file = new File(STORE_FILE);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, store);
        } catch (IOException e) {
            log.error("Failed to save GHN shop configs to file.", e);
        }
    }
}
