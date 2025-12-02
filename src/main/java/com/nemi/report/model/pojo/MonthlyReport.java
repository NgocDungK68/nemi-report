package com.nemi.report.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReport {
    private BigDecimal revenue;
    private BigDecimal todayRevenue;
    private BigDecimal adCost;
    private BigDecimal adCostPerOrder;
    private BigDecimal adCostPerRevenue;
    private Long orders;
    private Long returnedOrders;
}
