package com.nemi.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConfirmOrderWhen {
    UPDATE_STATUS_TO_NEW("update_status_to_new"),
    UPDATE_STATUS_TO_CONFIRM("update_status_to_confirm"),
    MOVE_TO_TRANSPORTER("move_to_transporter"),
    RECONCILIATED("reconciliated");

    private final String code;
}
