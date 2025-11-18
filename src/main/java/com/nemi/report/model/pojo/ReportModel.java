package com.nemi.report.model.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportModel {
    private String reportDate; // dd/MM/yyyy

    private long orders;
    private long confirmedOrders;
    private long returnedOrders;
    private long successOrders;

    private BigDecimal revenue; // Doanh số
    private BigDecimal trueRevenue; // Doanh thu
    private BigDecimal profit; // Lợi nhuận = trueRevenue - adCost

    private BigDecimal adCost;
    private BigDecimal adCostPerOrder;
    private BigDecimal adCostPerConfirmedOrder;
    private Double adCostPerRevenue;
}
