package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OverviewDataType {
    REVENUE("REVENUE", "Revenue data"),
    AD_COST("AD_COST", "Advertisement cost data"),
    AD_COST_PER_REVENUE("AD_COST_PER_REVENUE", "Advertisement cost per revenue data"),
    RETURNED_ORDER("RETURNED_ORDER", "Returned order data"),
    PROFIT("PROFIT", "Profit data");

    private final String code;
    private final String description;

    public static OverviewDataType fromCode(String code) {
        for (OverviewDataType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown data type code: " + code);
    }
}
