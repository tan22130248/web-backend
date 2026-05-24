package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.CategoryDto;
import com.fashion.auth.model.Category;
import com.fashion.auth.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** GET /api/categories */
    @GetMapping
    public ResponseEntity<?> getCategories() {
        try {
            List<CategoryDto> result = categoryRepository.findByIsActiveTrue()
                    .stream().map(CategoryDto::from).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/categories/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategory(@PathVariable String id) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            return ResponseEntity.ok(CategoryDto.from(category));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
