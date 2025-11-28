package com.nemi.report.model.ads_manager;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdsCostOfProductsResponse {
    private String currency;
    private List<AdsCostOfProduct> adsCostOfProducts;
    private BigDecimal totalSpent;
}
