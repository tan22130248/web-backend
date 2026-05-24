package com.fashion.auth.dto;

import com.fashion.auth.model.Category;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryDto {
    private String id;
    private String name;
    private String slug;
    private String parentId;
    private String parentName;
    private String imgUrl;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static CategoryDto from(Category c) {
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setSlug(c.getSlug());
        dto.setImgUrl(c.getImgUrl());
        dto.setActive(c.isActive());
        dto.setCreatedAt(c.getCreatedAt());
        if (c.getParent() != null) {
            dto.setParentId(c.getParent().getId());
            dto.setParentName(c.getParent().getName());
        }
        return dto;
    }
}
