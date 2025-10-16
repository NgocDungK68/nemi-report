package com.nemi.report.model.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class UpdateMonthlyTargetRequest {

    @NotNull
    private BigDecimal targetRevenue;

    @NotNull
    private BigDecimal targetAdCostPerRevenue;

    @NotNull
    private BigDecimal targetReturnedOrderPercent;
}
