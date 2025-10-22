package com.nemi.report.model.response;

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
public class MonthlyTargetResponse {

    @JsonProperty("totalRevenue")
    private BigDecimal totalRevenue;

    @JsonProperty("todayRevenue")
    private BigDecimal todayRevenue;

    @JsonProperty("todayRevenuePercent")
    private BigDecimal todayRevenuePercent;

    @JsonProperty("remainingRevenueNeeded")
    private BigDecimal remainingRevenueNeeded;

    @JsonProperty("adCost")
    private BigDecimal adCost;

    @JsonProperty("adCostPerRevenue")
    private BigDecimal adCostPerRevenue;

    @JsonProperty("targetAdCostPerRevenue")
    private BigDecimal targetAdCostPerRevenue;

    @JsonProperty("adCostPerOrder")
    private BigDecimal adCostPerOrder;

    @JsonProperty("order")
    private BigDecimal order;

    @JsonProperty("soldProduct")
    private BigDecimal soldProduct;

    @JsonProperty("returnedOrder")
    private BigDecimal returnedOrder;

    @JsonProperty("targetReturnedOrder")
    private BigDecimal targetReturnedOrder;

    @JsonProperty("targetReturnedOrderPercent")
    private BigDecimal targetReturnedOrderPercent;

    @JsonProperty("targetRevenue")
    private BigDecimal targetRevenue;

    @JsonProperty("targetRevenueProcess")
    private BigDecimal targetRevenueProcess;
}
