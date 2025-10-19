package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompareWithType {
    L7D("L7D", "Last 7 days", 7),
    L1M("L1M", "Last 1 month", 30),
    L2M("L2M", "Last 2 months", 60),
    L3M("L3M", "Last 3 months", 90),
    LY("LY", "Last year", 365);

    private final String code;
    private final String description;
    private final int days;

    public static CompareWithType fromCode(String code) {
        for (CompareWithType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown compare with code: " + code);
    }
}
