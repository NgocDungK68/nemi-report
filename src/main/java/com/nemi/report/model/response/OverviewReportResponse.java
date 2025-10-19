package com.nemi.report.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OverviewReportResponse {

    @JsonProperty("totalRevenue")
    private RevenueData totalRevenue;

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
    public static class RevenueData {
        @JsonProperty("revenue")
        private BigDecimal revenue;

        @JsonProperty("revenueChangePercent")
        private BigDecimal revenueChangePercent;

        @JsonProperty("orders")
        private BigDecimal orders;

        @JsonProperty("ordersChangePercent")
        private BigDecimal ordersChangePercent;
    }

    @Data
    public static class OrderData {
        @JsonProperty("revenue")
        private BigDecimal revenue;

        @JsonProperty("revenueChangePercent")
        private BigDecimal revenueChangePercent;

        @JsonProperty("orders")
        private BigDecimal orders;

        @JsonProperty("ordersChangePercent")
        private BigDecimal ordersChangePercent;
    }

    @Data
    public static class CostData {
        @JsonProperty("cost")
        private BigDecimal cost;

        @JsonProperty("costChangePercent")
        private BigDecimal costChangePercent;

        @JsonProperty("adCostPerRevenue")
        private BigDecimal adCostPerRevenue;

        @JsonProperty("adCostPerRevenueChangePercent")
        private BigDecimal adCostPerRevenueChangePercent;
    }

    @Data
    public static class ProfitData {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("changePercent")
        private BigDecimal changePercent;
    }
}
