package com.fashion.auth.controller;

import com.fashion.auth.dto.seller.product.ProductSaveRequestDto;
import com.fashion.auth.dto.seller.product.ProductSellerDto;
import com.fashion.auth.dto.seller.SellerRegistrationRequestDto;
import com.fashion.auth.model.Product;
import com.fashion.auth.repository.ProductVariantRepository;
import com.fashion.auth.security.JwtUtils;
import com.fashion.auth.service.ProductService;
import com.fashion.auth.service.SellerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seller")
public class SellerController {
    private final ProductService productService;
    private final ProductVariantRepository productVariantRepository;
    private final JwtUtils jwtUtils;
    private final SellerService sellerService;

    public SellerController(ProductService productService, ProductVariantRepository productVariantRepository, JwtUtils jwtUtils, SellerService sellerService) {
        this.productService = productService;
        this.productVariantRepository = productVariantRepository;
        this.jwtUtils = jwtUtils;
        this.sellerService = sellerService;
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerAsSeller(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody SellerRegistrationRequestDto request) {

        String email = extractEmail(authHeader);
        sellerService.registerAsSeller(email, request);

        return ResponseEntity.ok(Map.of(
            "message", "Đăng ký người bán thành công! Chúng tôi sẽ xem xét đơn yêu cầu của bạn trong vòng 24 giờ.",
            "status", "success"
        ));
    }

    @GetMapping("/check-seller-status")
    public ResponseEntity<Map<String, Object>> checkSellerStatus(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        String email = extractEmail(authHeader);
        boolean isSeller = sellerService.isUserSeller(email);
        String registrationStatus = sellerService.getSellerRegistrationStatus(email);

        return ResponseEntity.ok(Map.of(
            "isSeller", isSeller,
            "registrationStatus", registrationStatus
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductSellerDto>> getSellerProducts(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status) {

        String email = extractEmail(authHeader);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Product> products = productService.getProductsBySeller(email, keyword, status, pageable);
        Page<ProductSellerDto> result = products.map(ProductSellerDto::from);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductSellerDto> createProduct(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody ProductSellerDto productData) {

        String email = extractEmail(authHeader);
        Product newProduct = productService.createProductBySeller(email, productData);
        return ResponseEntity.ok(ProductSellerDto.from(newProduct));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductSellerDto> getProductDetail(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable String id) {

        String email = extractEmail(authHeader);
        ProductSellerDto product = productService.getProductDetailBySeller(id, email);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductSellerDto> updateProduct(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable String id,
            @RequestBody ProductSaveRequestDto requestDto) {

        String email = extractEmail(authHeader);
        ProductSellerDto savedDto = productService.updateProductBySeller(id, email, requestDto);
        return ResponseEntity.ok(savedDto);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable String id) {

        String email = extractEmail(authHeader);
        productService.softDeleteProductBySeller(id, email);

        return ResponseEntity.ok(Map.of("message", "Sản phẩm đã được ẩn thành công"));
    }

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportInventoryReport(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {

        String email = extractEmail(authHeader);

        byte[] csvBytes = productService.exportInventoryToCsv(email);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_report.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }
}