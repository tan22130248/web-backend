package com.fashion.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageSearchResultDto {
    private ProductDto product;
    private double similarity;
    /** Convenience field for the UI badge, e.g. 87 -> "TƯƠNG ĐỒNG 87%". */
    private int similarityPercent;

    public static ImageSearchResultDto of(ProductDto product, double similarity) {
        int percent = (int) Math.round(Math.max(0, Math.min(1, similarity)) * 100);
        return new ImageSearchResultDto(product, similarity, percent);
    }
}
