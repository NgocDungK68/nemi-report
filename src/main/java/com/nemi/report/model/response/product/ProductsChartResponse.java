package com.nemi.report.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductsChartResponse {
    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("userData")
    private List<UserData> userData;

    @JsonProperty("summary")
    private Summary summary;

    @Data
    public static class UserData {
        @JsonProperty("product")
        private ProductData product;

        @JsonProperty("date")
        private DateData date;

        @JsonProperty("dateValues")
        private List<DateValues> dateValues;
    }

    @Data
    public static class ProductData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class DateData {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }

    @Data
    public static class DateValues {
        @JsonProperty("date")
        private String date; // dd/MM/yyyy format

        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }

    @Data
    public static class Summary {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }
}
