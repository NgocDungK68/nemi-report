package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Currency {
    VND("VND"),
    USD("USD");

    private final String code;
}
