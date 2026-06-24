package com.fashion.auth.repository;

import com.fashion.auth.model.ProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCommentRepository extends JpaRepository<ProductComment, String> {

    Page<ProductComment> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    Page<ProductComment> findByProductIdAndIsBuyerTrueOrderByCreatedAtDesc(String productId, Pageable pageable);
}
