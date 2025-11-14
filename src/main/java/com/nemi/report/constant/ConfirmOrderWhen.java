package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum ConfirmOrderWhen {
    UPDATE_STATUS_TO_NEW("UPDATE_STATUS_TO_NEW"),
    UPDATE_STATUS_TO_CONFIRMED("UPDATE_STATUS_TO_CONFIRMED"),
    MOVE_TO_TRANSPORTER("MOVE_TO_TRANSPORTER"),
    RECONCILED("RECONCILED");

    private final String code;

    public static ConfirmOrderWhen fromCode(String code) {
        for (ConfirmOrderWhen type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown confirm order when code: " + code);
    }

    public List<String> getOrderStatus() {
        return switch (this) {
            case UPDATE_STATUS_TO_NEW -> List.of(OrderStatus.NEW.getCode(),
                    OrderStatus.NEW.getCode(),
                    OrderStatus.PROCESSING.getCode(),
                    OrderStatus.READY_TO_SHIP.getCode(),
                    OrderStatus.SHIPPING.getCode(),
                    OrderStatus.DELIVERED.getCode(),
                    OrderStatus.RETURNED.getCode()
                    );
            case UPDATE_STATUS_TO_CONFIRMED -> List.of(
                    OrderStatus.PROCESSING.getCode(),
                    OrderStatus.READY_TO_SHIP.getCode(),
                    OrderStatus.SHIPPING.getCode(),
                    OrderStatus.DELIVERED.getCode(),
                    OrderStatus.RETURNED.getCode()
            );
            case MOVE_TO_TRANSPORTER -> List.of(
                    OrderStatus.SHIPPING.getCode(),
                    OrderStatus.DELIVERED.getCode()
            );
            case RECONCILED -> List.of(OrderStatus.DELIVERED.getCode());
        };
    }
}
