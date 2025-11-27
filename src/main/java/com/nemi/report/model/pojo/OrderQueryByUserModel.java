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
public class OrderQueryByUserModel {

    // user
    private String userId;
    private String name;
    private String image;

    // order
    private long orders;
    private long confirmedOrders;
    private long returnedOrders;
    private long successOrders;

    private BigDecimal revenue; // Doanh số
    private BigDecimal trueRevenue; // Doanh thu
}
