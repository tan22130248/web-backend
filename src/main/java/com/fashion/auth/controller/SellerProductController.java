package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.ProductDto;
import com.fashion.auth.model.Product;
import com.fashion.auth.model.ProductImage;
import com.fashion.auth.model.Shop;
import com.fashion.auth.model.User;
import com.fashion.auth.repository.CategoryRepository;
import com.fashion.auth.repository.ProductRepository;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/seller/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SellerProductController {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public SellerProductController(ProductRepository productRepository, ShopRepository shopRepository, CategoryRepository categoryRepository, UserRepository userRepository, JwtUtils jwtUtils) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    private Shop getShop(String token) {
        String jwt = token.replace("Bearer ", "");
        String email = jwtUtils.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return shopRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Shop not found"));
    }

    @GetMapping
    public ResponseEntity<?> getSellerProducts(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "10") Integer lowStockThreshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Shop shop = getShop(token);
            Specification<Product> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("shop").get("id"), shop.getId()));

                if (search != null && !search.isBlank()) {
                    String keyword = "%" + search.toLowerCase() + "%";
                    Predicate nameMatch = cb.like(cb.lower(root.get("name")), keyword);
                    Predicate descMatch = cb.like(cb.lower(root.get("description")), keyword);
                    predicates.add(cb.or(nameMatch, descMatch));
                }
                if (status != null && !status.isBlank()) {
                    if ("active".equalsIgnoreCase(status)) {
                        predicates.add(cb.isTrue(root.get("isActive")));
                    } else if ("hidden".equalsIgnoreCase(status)) {
                        predicates.add(cb.isFalse(root.get("isActive")));
                    }
                }
                if (categoryId != null && !categoryId.isBlank()) {
                    predicates.add(cb.equal(root.get("category").get("id"), categoryId));
                }
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
                }
                if (stockStatus != null && !stockStatus.isBlank()) {
                    if ("in_stock".equalsIgnoreCase(stockStatus)) {
                        predicates.add(cb.greaterThan(root.get("stock"), lowStockThreshold));
                    } else if ("low_stock".equalsIgnoreCase(stockStatus)) {
                        predicates.add(cb.and(
                                cb.lessThanOrEqualTo(root.get("stock"), lowStockThreshold),
                                cb.greaterThan(root.get("stock"), 0)
                        ));
                    } else if ("out_of_stock".equalsIgnoreCase(stockStatus)) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("stock"), 0));
                    }
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Product> products = productRepository.findAll(spec, pageable);
            return ResponseEntity.ok(products.map(ProductDto::from));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            Shop shop = getShop(token);
            Product product = new Product();
            product.setShop(shop);
            product.setName((String) body.get("name"));
            product.setDescription((String) body.get("description"));
            product.setPrice(new java.math.BigDecimal(body.get("price").toString()));
            product.setStock((Integer) body.get("stock"));
            
            if (body.containsKey("conditionStatus")) {
                product.setConditionStatus((String) body.get("conditionStatus"));
            }
            if (body.containsKey("categoryId")) {
                product.setCategory(categoryRepository.findById((String) body.get("categoryId"))
                        .orElseThrow(() -> new RuntimeException("Category not found")));
            }
            
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) body.get("images");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                List<ProductImage> productImages = new ArrayList<>();
                for (int i = 0; i < imageUrls.size(); i++) {
                    ProductImage pi = new ProductImage();
                    pi.setProduct(product);
                    pi.setImageUrl(imageUrls.get(i));
                    pi.setSortOrder(i);
                    pi.setPrimary(i == 0);
                    productImages.add(pi);
                }
                product.setImages(productImages);
            }
            
            Product saved = productRepository.save(product);
            return ResponseEntity.ok(ProductDto.from(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            Shop shop = getShop(token);
            Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.getShop().getId().equals(shop.getId())) {
                throw new RuntimeException("Unauthorized");
            }

            if (body.containsKey("name")) product.setName((String) body.get("name"));
            if (body.containsKey("description")) product.setDescription((String) body.get("description"));
            if (body.containsKey("price")) product.setPrice(new java.math.BigDecimal(body.get("price").toString()));
            if (body.containsKey("stock")) product.setStock((Integer) body.get("stock"));
            if (body.containsKey("conditionStatus")) product.setConditionStatus((String) body.get("conditionStatus"));
            if (body.containsKey("categoryId")) {
                product.setCategory(categoryRepository.findById((String) body.get("categoryId"))
                        .orElseThrow(() -> new RuntimeException("Category not found")));
            }

            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) body.get("images");
            if (imageUrls != null) {
                if (product.getImages() != null) {
                    product.getImages().clear();
                } else {
                    product.setImages(new ArrayList<>());
                }
                for (int i = 0; i < imageUrls.size(); i++) {
                    ProductImage pi = new ProductImage();
                    pi.setProduct(product);
                    pi.setImageUrl(imageUrls.get(i));
                    pi.setSortOrder(i);
                    pi.setPrimary(i == 0);
                    product.getImages().add(pi);
                }
            }

            Product saved = productRepository.save(product);
            return ResponseEntity.ok(ProductDto.from(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        try {
            Shop shop = getShop(token);
            Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.getShop().getId().equals(shop.getId())) {
                throw new RuntimeException("Unauthorized");
            }
            if (body.containsKey("isActive")) {
                product.setActive(body.get("isActive"));
            }
            productRepository.save(product);
            return ResponseEntity.ok(ProductDto.from(product));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
