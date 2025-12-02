package com.nemi.report.model.ads_manager;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdsCostOfProduct {
    private String productId;
    private BigDecimal spent;
    private List<AdCostByDate> adCostByDate;
}
