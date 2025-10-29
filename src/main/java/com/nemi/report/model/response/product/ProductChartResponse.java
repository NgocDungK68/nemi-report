package com.nemi.report.model.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductChartResponse {
    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("dateData")
    private List<DateData> dateData;

    @JsonProperty("summary")
    private Summary summary;

    @Data
    public static class DateData {
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
