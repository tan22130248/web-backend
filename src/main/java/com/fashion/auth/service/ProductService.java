package com.fashion.auth.service;

import com.fashion.auth.dto.category.ProductFilterDto;
import com.fashion.auth.dto.seller.product.ProductSaveRequestDto;
import com.fashion.auth.dto.seller.product.ProductSellerDto;
import com.fashion.auth.model.Category;
import com.fashion.auth.model.Product;
import com.fashion.auth.model.ProductImage;
import com.fashion.auth.model.Shop;
import com.fashion.auth.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          ProductImageRepository productImageRepository,
                          ShopRepository shopRepository,
                          UserRepository userRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable);
    }

    public List<Product> getByIds(List<String> ids) {
        return productRepository.findAllById(ids);
    }

    public Page<Product> getByCategory(String categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
    }

    public Page<Product> getByShop(String shopId, Pageable pageable) {
        return productRepository.findByShopIdAndIsActiveTrue(shopId, pageable);
    }

    public Page<Product> search(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable);
    }

    public Product getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
        return product;
    }

    public List<ProductImage> getProductImages(String productId) {
        return productImageRepository.findByProductIdOrderBySortOrder(productId);
    }

    public Product createProduct(String email, Product product) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));
        product.setShop(shop);
        return productRepository.save(product);
    }

    public Product updateProduct(String email, String productId, Product updates) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));

        if (!product.getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Bạn không có quyền sửa sản phẩm này");
        }

        if (updates.getName() != null) product.setName(updates.getName());
        if (updates.getDescription() != null) product.setDescription(updates.getDescription());
        if (updates.getPrice() != null) product.setPrice(updates.getPrice());
        if (updates.getConditionStatus() != null) product.setConditionStatus(updates.getConditionStatus());
        product.setStock(updates.getStock());

        return productRepository.save(product);
    }

    public void deleteProduct(String email, String productId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));

        if (!product.getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Bạn không có quyền xoá sản phẩm này");
        }

        product.setActive(false);
        productRepository.save(product);
    }

    public List<String> getUniqueConditionStatuses() {
        return productRepository.getUniqueConditionStatuses().stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .toList();
    }

    public Page<Product> getFilteredProducts(ProductFilterDto filter) {

        Sort sort = Sort.by("soldCount").descending();

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Specification<Product> spec = filterProducts(filter);

        return productRepository.findAll(spec, pageable);
    }

    public Specification<Product> filterProducts(ProductFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getCategoryId() != null && !filter.getCategoryId().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            if (filter.getConditionStatus() != null && !filter.getConditionStatus().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("conditionStatus"), filter.getConditionStatus()));
            }

            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            predicates.add(criteriaBuilder.equal(root.get("isActive"), true));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    //    Seller
    public Page<Product> getProductsBySeller(String email, String keyword, String status, Pageable pageable) {
//        Shop shop = getShopByEmail(email);

        Shop shop = getShopByEmail(email);

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("shop").get("id"), shop.getId()));

            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + keyword.toLowerCase() + "%"
                ));
            }

            switch (status.toUpperCase()) {
                case "ACTIVE": // Tab Đang bán
                    predicates.add(criteriaBuilder.equal(root.get("isActive"), true));
                    predicates.add(criteriaBuilder.greaterThan(root.get("stock"), 0));
                    break;
                case "OUT_OF_STOCK": // Tab Hết hàng
                    predicates.add(criteriaBuilder.equal(root.get("stock"), 0));
                    predicates.add(criteriaBuilder.equal(root.get("isActive"), true));
                    break;
                case "HIDDEN": // Tab Đã ẩn
                    predicates.add(criteriaBuilder.equal(root.get("isActive"), false));
                    break;
                case "ALL": // Tab Tất cả (Ngoại trừ các sản phẩm đã bị xóa vĩnh viễn nếu có logic đó)
                default:
                    break;
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }

    @Transactional
    public Product createProductBySeller(String email, ProductSellerDto productData) {
        // 1. Khởi tạo và thiết lập Shop cố định (hoặc lấy từ email)
        Shop shop = getShopByEmail(email);

        // 2. Map dữ liệu từ DTO sang Entity Product
        Product product = new Product();
        product.setShop(shop);
        product.setName(productData.getName());
        product.setPrice(productData.getPrice());
        product.setStock(productData.getStock());
        product.setConditionStatus(productData.getConditionStatus());
        product.setDescription(productData.getDescription());
        product.setActive(true);
        product.setSoldCount(0);
        product.setViewCount(0);

        // Nếu có Category, bạn cần tìm và gán vào đây
        if (productData.getCategoryId() != null) {
            Category category = categoryRepository.findById(productData.getCategoryId()).orElse(null);
            product.setCategory(category);
        }

        // 3. Xử lý gom nhóm Ảnh bìa và Ảnh phụ thành danh sách ProductImage
        List<ProductImage> productImages = new ArrayList<>();

        int orderCounter = 0; // Biến đếm phục vụ sortOrder ASC

        if (productData.getImageUrl() != null && !productData.getImageUrl().isBlank()) {
            ProductImage mainImg = new ProductImage();
            mainImg.setImageUrl(productData.getImageUrl());
            mainImg.setProduct(product);
            mainImg.setPrimary(true);
            mainImg.setSortOrder(orderCounter++);
            productImages.add(mainImg);
        }

        // Xử lý danh sách ảnh phụ (sortOrder tăng dần: 1, 2, 3...)
        if (productData.getImages() != null) {
            for (String subImgUrl : productData.getImages()) {
                if (subImgUrl != null && !subImgUrl.isEmpty()) {
                    ProductImage subImg = new ProductImage();
                    subImg.setImageUrl(subImgUrl);
                    subImg.setProduct(product);
                    subImg.setPrimary(false);
                    subImg.setSortOrder(orderCounter++);
                    productImages.add(subImg);
                }
            }
        }

        // Gán danh sách ảnh hoàn chỉnh vào product
        product.setImages(productImages);

        // Gán danh sách ảnh đã map vào product
        product.setImages(productImages);

        // 4. Lưu product xuống DB (nhờ cascade sang bảng ProductImage)
        return productRepository.save(product);
    }

    public Product getProductByIdAndSeller(String id, String email) {
        Shop shop = getShopByEmail(email);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));

        if (!product.getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập sản phẩm này!");
        }

        return product;
    }

    /**
     * Hàm mới: Lấy chi tiết sản phẩm và map sang DTO phẳng để trả về cho Frontend
     */
    public ProductSellerDto getProductDetailBySeller(String id, String email) {
        Product product = getProductByIdAndSeller(id, email);

        return ProductSellerDto.from(product);
    }

    @Transactional
    public ProductSellerDto updateProductBySeller(String id, String email, ProductSaveRequestDto requestDto) {
        Product product = getProductByIdAndSeller(id, email);

        product.setName(requestDto.getName());
        product.setPrice(requestDto.getPrice());
        product.setStock(requestDto.getStock());
        product.setDescription(requestDto.getDescription());
        product.setConditionStatus(requestDto.getConditionStatus());

        productImageRepository.deleteByProductId(product.getId());

        List<ProductImage> newImages = new ArrayList<>();
        int order = 0;

        if (requestDto.getPrimaryImage() != null && !requestDto.getPrimaryImage().isBlank()) {
            ProductImage mainImg = ProductImage.builder()
                    .product(product)
                    .imageUrl(requestDto.getPrimaryImage())
                    .isPrimary(true)  // 🔥 Đánh dấu là ảnh chính
                    .sortOrder(order++)
                    .build();
            newImages.add(mainImg);
        }

        if (requestDto.getSubImages() != null && !requestDto.getSubImages().isEmpty()) {
            for (String url : requestDto.getSubImages()) {
                if (url != null && !url.isBlank()) {
                    ProductImage subImg = ProductImage.builder()
                            .product(product)
                            .imageUrl(url)
                            .isPrimary(false) // 🔥 Đánh dấu là ảnh phụ
                            .sortOrder(order++)
                            .build();
                    newImages.add(subImg);
                }
            }
        }

        product.setImages(newImages);

        Product savedProduct = productRepository.save(product);
        return ProductSellerDto.from(savedProduct);
    }

    @Transactional // Thêm @Transactional vì có hành động xóa và lưu nhiều bảng
    public void softDeleteProductBySeller(String id, String email) {
        Product product = getProductByIdAndSeller(id, email);

        product.setActive(false);
        productRepository.save(product);
    }

    public byte[] exportInventoryToCsv(String email) {
//        Shop shop = getShopByEmail(email);

        Shop shop = getShopByEmail(email);

        List<Product> products = productRepository.findByShopId(shop.getId());

        StringBuilder csvContent = new StringBuilder();

        csvContent.append('\ufeff');

        csvContent.append("ID Sản Phẩm,Tên Sản Phẩm,Giá (VND),Số Lượng Kho,Trạng Thái\n");

        for (Product p : products) {
            String status = p.getStock() == 0 ? "Hết hàng" : (p.isActive() ? "Đang bán" : "Đã ẩn");
            csvContent.append(String.format("%s,\"%s\",%.0f,%d,%s\n",
                    p.getId(),
                    p.getName().replace("\"", "\"\""), // Escape dấu nháy kép nếu tên sản phẩm có chứa nó
                    p.getPrice(),
                    p.getStock(),
                    status
            ));
        }

        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Shop getShopByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có cửa hàng"));
    }
}
