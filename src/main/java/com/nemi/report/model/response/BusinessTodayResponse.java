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
public class BusinessTodayResponse {

    @JsonProperty("revenue")
    private BigDecimal revenue;

    @JsonProperty("adCost")
    private BigDecimal adCost;

    @JsonProperty("adCostPerRevenue")
    private BigDecimal adCostPerRevenue;

    @JsonProperty("confirmedOrder")
    private OrderData confirmedOrder;

    @JsonProperty("deliveredOrder")
    private OrderData deliveredOrder;

    @JsonProperty("pendingOrder")
    private OrderData pendingOrder;

    @JsonProperty("canceledOrder")
    private OrderData canceledOrder;

    @JsonProperty("revenuePerHourFrame")
    private List<HourFrameData> revenuePerHourFrame;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderData {
        @JsonProperty("revenue")
        private BigDecimal revenue;

        @JsonProperty("orders")
        private BigDecimal orders;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HourFrameData {
        @JsonProperty("hourFrame")
        private String hourFrame;

        @JsonProperty("value")
        private BigDecimal value;
    }
}
