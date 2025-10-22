package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ColumnLegend {
    VND("VND"),
    USD("USD"),
    PERCENT("%");

    private final String code;
}
