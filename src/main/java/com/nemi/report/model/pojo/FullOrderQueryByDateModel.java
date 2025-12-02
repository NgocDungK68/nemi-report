package com.nemi.report.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullOrderQueryByDateModel {

    private String reportDate;

    private long orders; // ALL
    private long confirmedOrders;
    private long returnedOrders;
    private long deliveringOrders;
    private long pendingOrders;
    private long canceledOrders;

    private BigDecimal revenue; // Doanh số ALL
    private BigDecimal trueRevenue; // Doanh thu
    private BigDecimal confirmedRevenue;
    private BigDecimal returnedRevenue;
    private BigDecimal deliveringRevenue;
    private BigDecimal pendingRevenue;
    private BigDecimal canceledRevenue;
}
