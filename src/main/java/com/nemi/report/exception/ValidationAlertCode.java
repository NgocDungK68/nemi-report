package com.nemi.report.exception;


import com.nemi.exception.constant.AlertType;
import com.nemi.exception.pojo.AlertCode;
import com.nemi.exception.pojo.IAlertCode;

/**
 * Các loại lỗi, thuộc về Validation
 */
public enum ValidationAlertCode implements IAlertCode {
    DATA_INVALID("400001", "Giá trị đầu vào không hợp lệ", AlertType.ERROR),
    ;

    private final AlertCode alertCode;

    ValidationAlertCode(final String code, final String label, final AlertType alertType) {
        alertCode = new AlertCode(code, label, alertType);
    }

    @Override
    public String getCode() {
        return alertCode.getCode();
    }

    @Override
    public String getLabel() {
        return alertCode.getLabel();
    }

    @Override
    public AlertType getType() {
        return alertCode.getType();
    }

    @Override
    public String getUserMessage() {
        return "";
    }

    @Override
    public String getErrorSubCode() {
        return "";
    }
}
