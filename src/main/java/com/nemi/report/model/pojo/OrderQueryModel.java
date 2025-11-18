package com.nemi.report.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueryModel {

    private String reportDate;
    private long orders;
    private long confirmedOrders;
    private long returnedOrders;
    private long successOrders;

    private BigDecimal revenue; // Doanh số
    private BigDecimal trueRevenue; // Doanh thu
}
