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
@AllArgsConstructor
@NoArgsConstructor
public class ProductChartResponse {
    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("dateData")
    private List<DateData> dateData;

    @JsonProperty("summary")
    private Summary summary;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DateData {
        @JsonProperty("date")
        private LocalDate date;

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
