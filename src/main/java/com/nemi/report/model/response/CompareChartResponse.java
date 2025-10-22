package com.nemi.report.model.response;

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

    @JsonProperty("columnLegend")
    private String columnLegend;

    @Data
    @Builder
    public static class ChartDataPoint {
        @JsonProperty("date")
        private String date; // dd/MM/yyyy

        @JsonProperty("presentValue")
        private BigDecimal presentValue;

        @JsonProperty("previousValue")
        private BigDecimal previousValue;

        @JsonProperty("changePercent")
        private BigDecimal changePercent;

        @JsonProperty("additions")
        private List<Addition> additions;
    }

    @Data
    public static class Addition {
        @JsonProperty("order")
        private Integer order;

        @JsonProperty("titleEn")
        private String titleEn;

        @JsonProperty("titleVi")
        private String titleVi;

        @JsonProperty("value")
        private String value;
    }
}
