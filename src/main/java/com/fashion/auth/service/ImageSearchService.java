package com.fashion.auth.service;

import com.fashion.auth.dto.ImageSearchResultDto;
import com.fashion.auth.dto.ProductDto;
import com.fashion.auth.model.Product;
import com.fashion.auth.repository.ProductRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImageSearchService {

    private final RestClient imageSearchRestClient;
    private final ProductRepository productRepository;

    public ImageSearchService(RestClient imageSearchRestClient,
                              ProductRepository productRepository) {
        this.imageSearchRestClient = imageSearchRestClient;
        this.productRepository = productRepository;
    }

    @SuppressWarnings("unchecked")
    public List<ImageSearchResultDto> searchByImage(MultipartFile file, int topK, double minSimilarity) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Vui lòng tải lên một ảnh hợp lệ");
        }

        // ── 1. Call the Python service (multipart upload) ──────────────────
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    String name = file.getOriginalFilename();
                    return name != null ? name : "query.jpg";
                }
            };
            body.add("file", resource);
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được dữ liệu ảnh");
        }

        Map<String, Object> response;
        try {
            response = imageSearchRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("topK", topK)
                            .queryParam("minSimilarity", minSimilarity)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Dịch vụ tìm kiếm bằng hình ảnh hiện không khả dụng");
        }

        if (response == null || !response.containsKey("results")) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        // ── 2. Preserve rank order while batch-fetching products ───────────
        LinkedHashMap<String, Double> idToSimilarity = new LinkedHashMap<>();
        for (Map<String, Object> r : results) {
            Object pid = r.get("productId");
            Object sim = r.get("similarity");
            if (pid != null && sim != null) {
                idToSimilarity.put(pid.toString(), ((Number) sim).doubleValue());
            }
        }

        Map<String, Product> productById = new LinkedHashMap<>();
        for (Product p : productRepository.findAllById(idToSimilarity.keySet())) {
            productById.put(p.getId(), p);
        }

        List<ImageSearchResultDto> hydrated = new ArrayList<>();
        for (Map.Entry<String, Double> entry : idToSimilarity.entrySet()) {
            Product product = productById.get(entry.getKey());
            // Skip products that no longer exist or have been hidden.
            if (product == null || !product.isActive()) {
                continue;
            }
            hydrated.add(ImageSearchResultDto.of(ProductDto.from(product), entry.getValue()));
        }
        return hydrated;
    }
}
