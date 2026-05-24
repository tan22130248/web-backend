package com.fashion.auth.repository;

import com.fashion.auth.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByIsActiveTrue();
    List<Category> findByParentIsNull();
}
