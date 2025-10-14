package com.nemi.report.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BusinessTodayResponse {

    @JsonProperty("revenue")
    private BigDecimal revenue;

    @JsonProperty("adCost")
    private BigDecimal adCost;

    @JsonProperty("adCostPerRevenue")
    private BigDecimal adCostPerRevenue;

    @JsonProperty("confirmedOrder")
    private BigDecimal confirmedOrder;

    @JsonProperty("deliveredOrder")
    private BigDecimal deliveredOrder;

    @JsonProperty("pendingOrder")
    private BigDecimal pendingOrder;

    @JsonProperty("canceledOrder")
    private BigDecimal canceledOrder;

    @JsonProperty("revenuePerHourFrame")
    private List<HourFrameData> revenuePerHourFrame;

    @Data
    public static class HourFrameData {
        @JsonProperty("hourFrame")
        private String hourFrame;

        @JsonProperty("value")
        private BigDecimal value;
    }
}
