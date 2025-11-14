package com.nemi.report.model.ads_manager;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdCostByDate {
    private String reportDate;
    private BigDecimal spent;
}
