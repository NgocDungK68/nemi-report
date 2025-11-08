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

    public static ColumnLegend fromCode(String code) {
        for (ColumnLegend value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown column legend when code: " + code);
    }
}
