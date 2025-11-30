package com.nemi.report.model.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportModel {

    protected long orders;
    protected long confirmedOrders;
    protected long returnedOrders;
    protected long successOrders;

    protected BigDecimal revenue; // Doanh số
    protected BigDecimal trueRevenue; // Doanh thu
    protected BigDecimal returnedRevenue; // Doanh số đơn hoàn
    protected BigDecimal profit; // Lợi nhuận = trueRevenue - adCost

    protected BigDecimal adCost;
    protected BigDecimal adCostPerOrder;
    protected BigDecimal adCostPerConfirmedOrder;
    protected Double adCostPerRevenue;
}
