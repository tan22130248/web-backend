package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.ImageSearchResultDto;
import com.fashion.auth.dto.ProductBatchDto;
import com.fashion.auth.dto.ProductDto;
import com.fashion.auth.dto.category.ProductFilterDto;
import com.fashion.auth.model.Product;
import com.fashion.auth.model.ProductVariant;
import com.fashion.auth.repository.ProductVariantRepository;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.service.ImageSearchService;
import com.fashion.auth.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {

    private final ProductService productService;
    private final ProductVariantRepository productVariantRepository;
    private final JwtUtils jwtUtils;
    private final ImageSearchService imageSearchService;

    public ProductController(ProductService productService,
                             ProductVariantRepository productVariantRepository,
                             JwtUtils jwtUtils,
                             ImageSearchService imageSearchService) {
        this.productService = productService;
        this.productVariantRepository = productVariantRepository;
        this.jwtUtils = jwtUtils;
        this.imageSearchService = imageSearchService;
    }


    /** GET /api/products?page=0&size=20&category=&keyword=&shop= */
    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String shop) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Product> products;

            if (keyword != null && !keyword.isBlank()) {
                products = productService.search(keyword.trim(), pageable);
            } else if (category != null && !category.isBlank()) {
                products = productService.getByCategory(category, pageable);
            } else if (shop != null && !shop.isBlank()) {
                products = productService.getByShop(shop, pageable);
            } else {
                products = productService.getAllProducts(pageable);
            }

            Page<ProductDto> dtoPage = products.map(ProductDto::from);
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/products/trusted-latest?page=0&size=12 */
    @GetMapping("/trusted-latest")
    public ResponseEntity<Page<ProductDto>> getTrustedLatestProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getTrustedLatestProducts(pageable).map(ProductDto::from));
    }

    /** GET /api/products/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        try {
            Product product = productService.getById(id);
            return ResponseEntity.ok(ProductDto.from(product));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** GET /api/products/{id}/variants */
    @GetMapping("/{id}/variants")
    public ResponseEntity<?> getProductVariants(@PathVariable String id) {
        try {
            List<ProductVariant> variants = productVariantRepository.findByProductId(id);
            return ResponseEntity.ok(variants);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/products/batch — Refresh cart items (public) */
    @PostMapping("/batch")
    public ResponseEntity<?> getProductsBatch(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> ids = body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Danh sách ids không được trống"));
            }
            List<ProductBatchDto> result = productService.getByIds(ids).stream()
                    .map(ProductBatchDto::from)
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** POST /api/products (Seller only) */
    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestHeader("Authorization") String token,
            @RequestBody Product product) {
        try {
            String email = extractEmail(token);
            Product created = productService.createProduct(email, product);
            return ResponseEntity.ok(ProductDto.from(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** PUT /api/products/{id} (Seller only, owner) */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody Product product) {
        try {
            String email = extractEmail(token);
            Product updated = productService.updateProduct(email, id, product);
            return ResponseEntity.ok(ProductDto.from(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /** DELETE /api/products/{id} (Seller only, owner) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            String email = extractEmail(token);
            productService.deleteProduct(email, id);
            return ResponseEntity.ok(new MessageResponse("Đã ẩn sản phẩm"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private String extractEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ");
        }
        String jwt = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(jwt);
        if (email == null || !jwtUtils.validateToken(jwt)) {
            throw new RuntimeException("Token không hợp lệ");
        }
        return email;
    }

    @GetMapping("/conditions")
    public ResponseEntity<List<String>> getProductConditions() {
        return ResponseEntity.ok(productService.getUniqueConditionStatuses());
    }

    /**
     * Endpoint lọc sản phẩm nâng cao
     * GET /api/products/filter?categoryId=...&conditionStatus=...&minPrice=...&maxPrice=...&page=0&size=9
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductDto>> filterProducts(ProductFilterDto filterRequest) {
        return ResponseEntity.ok(productService.getFilteredProducts(filterRequest).map(ProductDto::from));
    }

    /**
     * Tìm kiếm sản phẩm bằng hình ảnh (public).
     * POST /api/products/image-search  (multipart/form-data, field "file")
     * Trả về danh sách sản phẩm kèm độ tương đồng, đã sắp xếp giảm dần.
     */
    @PostMapping(value = "/image-search", consumes = "multipart/form-data")
    public ResponseEntity<?> searchByImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "24") int topK,
            @RequestParam(defaultValue = "0.15") double minSimilarity) {
        try {
            List<ImageSearchResultDto> results =
                    imageSearchService.searchByImage(file, topK, minSimilarity);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}


