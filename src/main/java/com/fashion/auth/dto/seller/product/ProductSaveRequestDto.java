package com.fashion.auth.dto.seller.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSaveRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String conditionStatus;
    private String categoryId; // Dùng ID để map Category phía Backend

    // 🔥 Tách biệt ảnh chính và ảnh phụ từ Request gửi lên
    private String primaryImage;       // Chứa 1 URL của ảnh chính
    private List<String> subImages;    // Chứa danh sách URL của các ảnh phụ
}