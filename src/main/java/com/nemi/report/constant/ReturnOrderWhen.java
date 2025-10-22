package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReturnOrderWhen {
    RETURN("return"),
    RETURNED("returned");

    private final String code;
}
