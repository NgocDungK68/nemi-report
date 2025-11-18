package com.nemi.report.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketingChartType {
    REVENUE("revenue"),
    ORDERS("orders"),
    AD_COST("ad_cost"),
    AD_COST_PER_REVENUE("ad_cost_per_revenue"),
    AD_COST_PER_ORDER("ad_cost_per_order"),
    PROFIT("profit");

    private final String value;
}
