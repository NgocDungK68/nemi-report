package com.nemi.report.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.report.entity.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
public class ProductSummaryResponse {
    @JsonProperty("totalElements")
    private Long totalElements;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("data")
    private List<DataItem> data;

    @JsonProperty("summary")
    private Map<String, Object> summary;

    @Data
    public static class DataItem {
        @JsonProperty("product")
        private ProductData product;

        @JsonProperty("extraData")
        private Map<String, Object> extraData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("image")
        private String imageUrl;
    }

    public static List<ProductData> toProductData(List<ProductEntity> productEntities){
        return productEntities.stream().map(entity -> {
            ProductData productData = new ProductData();
            productData.setId(entity.getProductId());
            productData.setName(entity.getName());
            productData.setImageUrl(entity.getImages());
            return productData;
        }).toList();

    }
}
