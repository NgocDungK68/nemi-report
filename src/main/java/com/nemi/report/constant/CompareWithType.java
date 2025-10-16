package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompareWithType {
    L7D("L7D", "Last 7 days"),
    L1M("L1M", "Last 1 month"),
    L2M("L2M", "Last 2 months"),
    L3M("L3M", "Last 3 months"),
    LY("LY", "Last year");

    private final String code;
    private final String description;

    public static CompareWithType fromCode(String code) {
        for (CompareWithType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown compare with code: " + code);
    }
}
