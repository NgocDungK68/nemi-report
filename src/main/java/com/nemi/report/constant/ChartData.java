package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChartData {
    REVENUE("revenue"),
    CONFIRMED_ORDER("confirmed_orders"),
    CANCELED_ORDER("cancelled_orders"),
    RETURNED_ORDER("returned_orders"),
    AD_COST("ad_cost"),
    PROFIT("profit"),
    CONFIRMED_ORDER_PERCENT("confirmed_order_percent"),
    CANCELED_ORDER_PERCENT("cancelled_order_percent"),
    RETURNED_ORDER_PERCENT("returned_order_percent"),
    AD_COST_PER_REVENUE("ad_cost_per_revenue"),;

    private final String code;
}
