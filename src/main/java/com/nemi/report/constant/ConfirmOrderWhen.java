package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum ConfirmOrderWhen {
    UPDATE_STATUS_TO_NEW("update_status_to_new"),
    UPDATE_STATUS_TO_CONFIRM("update_status_to_confirm"),
    MOVE_TO_TRANSPORTER("move_to_transporter"),
    RECONCILIATED("reconciliated");

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
            case UPDATE_STATUS_TO_NEW -> List.of(OrderStatus.NEW.getCode());
            case UPDATE_STATUS_TO_CONFIRM -> List.of(
                    OrderStatus.PROCESSING.getCode(),
                    OrderStatus.READY_TO_SHIP.getCode()
            );
            case MOVE_TO_TRANSPORTER -> List.of(
                    OrderStatus.SHIPPING.getCode(),
                    OrderStatus.DELIVERED.getCode()
            );
            case RECONCILIATED -> List.of(OrderStatus.DELIVERED.getCode());
        };
    }
}
