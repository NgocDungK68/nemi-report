package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Limit {
    TOP5(5),
    TOP10(10),
    ALL(Integer.MAX_VALUE);

    private final int value;
}
