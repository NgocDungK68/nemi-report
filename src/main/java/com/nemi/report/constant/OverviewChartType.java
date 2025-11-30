package com.nemi.report.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OverviewChartType {
    REVENUE("revenue", false),
    RETURNED_REVENUE("returned_revenue", false),
    AD_COST("ad_cost", true),
    AD_COST_PER_REVENUE("ad_cost_per_revenue", true),
    PROFIT("profit", true);

    private final String value;
    private final boolean isContainAdsCost;
}
