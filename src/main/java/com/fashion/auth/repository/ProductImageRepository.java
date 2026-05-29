package com.fashion.auth.repository;

import com.fashion.auth.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductIdOrderBySortOrder(String productId);

    void deleteByProductId(String id);
}
