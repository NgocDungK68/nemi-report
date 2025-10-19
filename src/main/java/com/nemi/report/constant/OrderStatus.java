package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    NEW("NEW"),
    PROCESSING("PROCESSING"),
    READY_TO_SHIP("READY_TO_SHIP"),
    SHIPPING("SHIPPING"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    FAILED("FAILED"),
    RETURNED("RETURNED");

    private final String code;
}
