package com.nemi.report.model.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSumModel {
    private Long orders;
    private Long confirmedOrders;
    private Long successOrders;
    private Long returnedOrders;
    private BigDecimal revenue;
    private BigDecimal trueRevenue;
}
