package com.nemi.report.model.response.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverviewReportResponse {

    @JsonProperty("totalRevenue")
    private OrderData totalRevenue;

    @JsonProperty("returnedOrders")
    private OrderData returnedOrders;

    @JsonProperty("confirmedOrders")
    private OrderData confirmedOrders;

    @JsonProperty("deliveringOrders")
    private OrderData deliveringOrders;

    @JsonProperty("adCost")
    private CostData adCost;

    @JsonProperty("profit")
    private ProfitData profit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderData {
        @JsonProperty("revenue")
        private BigDecimal revenue;

        @JsonProperty("previousRevenue")
        private BigDecimal previousRevenue;

        @JsonProperty("revenueChangePercent")
        private BigDecimal revenueChangePercent;

        @JsonProperty("orders")
        private Long orders;

        @JsonProperty("previousOrders")
        private Long previousOrders;

        @JsonProperty("ordersChangePercent")
        private BigDecimal ordersChangePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostData {
        @JsonProperty("cost")
        private BigDecimal cost;

        @JsonProperty("previousCost")
        private BigDecimal previousCost;

        @JsonProperty("costChangePercent")
        private BigDecimal costChangePercent;

        @JsonProperty("adCostPerRevenue")
        private BigDecimal adCostPerRevenue;

        @JsonProperty("previousAdCostPerRevenue")
        private BigDecimal previousAdCostPerRevenue;

        @JsonProperty("adCostPerRevenueChangePercent")
        private BigDecimal adCostPerRevenueChangePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitData {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("previousValue")
        private BigDecimal previousValue;

        @JsonProperty("changePercent")
        private BigDecimal changePercent;
    }
}
