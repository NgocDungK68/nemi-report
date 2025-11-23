package com.nemi.report.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProductsChartResponse {
    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("productData")
    private List<ProductData> productData;

    @JsonProperty("summary")
    private Summary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductData {
        @JsonProperty("product")
        private Product product;

        @JsonProperty("data")
        private DataValue data;

        @JsonProperty("dateValues")
        private List<DateValue> dateValues;
    }

    @Data
    @Builder
    public static class Summary {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }

    @Data
    @Builder
    public static class Product {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }

    @Data
    @Builder
    public static class DataValue {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }

    @Data
    public static class DateValue {
        @JsonProperty("date")
        private LocalDate date; // dd/MM/yyyy format

        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }
}
