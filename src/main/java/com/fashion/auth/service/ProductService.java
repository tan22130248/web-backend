package com.fashion.auth.service;

import com.fashion.auth.model.Product;
import com.fashion.auth.model.ProductImage;
import com.fashion.auth.model.Shop;
import com.fashion.auth.repository.ProductImageRepository;
import com.fashion.auth.repository.ProductRepository;
import com.fashion.auth.repository.ShopRepository;
import com.fashion.auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository,
                          ProductImageRepository productImageRepository,
                          ShopRepository shopRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
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
}
