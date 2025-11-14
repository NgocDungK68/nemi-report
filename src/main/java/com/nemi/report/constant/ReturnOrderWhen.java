package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum ReturnOrderWhen {
    RETURNING("RETURNING"),
    RETURNED("RETURNED");

    private final String code;

    public static ReturnOrderWhen fromCode(String code) {
        for (ReturnOrderWhen type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown return order when code: " + code);
    }

    public List<String> getOrderStatus() {
        return switch (this) {
            case RETURNING -> List.of(
                    OrderStatus.RETURNED.getCode()
            );
            case RETURNED -> List.of(OrderStatus.RETURNED.getCode());
        };
    }
}
