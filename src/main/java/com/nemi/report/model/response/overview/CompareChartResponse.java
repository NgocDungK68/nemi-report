package com.nemi.report.model.response.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompareChartResponse {

    @JsonProperty("data")
    private List<ChartDataPoint> data;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartDataPoint {
        @JsonProperty("date")
        private String date; // dd/MM/yyyy

        private String previousDate; // dd/MM/yyyy

        @JsonProperty("presentValue")
        private BigDecimal presentValue;

        @JsonProperty("previousValue")
        private BigDecimal previousValue;

        @JsonProperty("changePercent")
        private BigDecimal changePercent;

        @JsonProperty("orders")
        private Long orders;
    }
}
