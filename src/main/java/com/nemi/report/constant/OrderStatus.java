package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

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

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status with code: " + code);
    }

    public static List<String> getTotalOrdersStatus() {
        return List.of(
                OrderStatus.NEW.getCode(),
                OrderStatus.PROCESSING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode(),
                OrderStatus.SHIPPING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode(),
                OrderStatus.DELIVERED.getCode()
        );
    }

    public static List<String> getReturnedOrdersStatus() {
        return List.of(
                OrderStatus.RETURNED.getCode(),
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.FAILED.getCode()
        );
    }

    public static List<String> getConfirmedOrdersStatus() {
        return List.of(
                OrderStatus.DELIVERED.getCode(),
                OrderStatus.SHIPPING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode()
        );
    }

    public static List<String> getDeliveringOrdersStatus() {
        return List.of(
                OrderStatus.DELIVERED.getCode()
        );
    }

    public static List<String> getCancelledOrdersStatus() {
        return List.of(
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.FAILED.getCode()
        );
    }
}
