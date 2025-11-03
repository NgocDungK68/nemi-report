package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Currency {
    VND("VND"),
    USD("USD");

    private final String code;

    public static Currency fromCode(String code) {
        for (Currency type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown currency code: " + code);
    }
}
