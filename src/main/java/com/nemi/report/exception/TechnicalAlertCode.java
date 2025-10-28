package com.nemi.report.exception;

import com.nemi.exception.constant.AlertType;
import com.nemi.exception.pojo.AlertCode;
import com.nemi.exception.pojo.IAlertCode;

/**
 * Các loại lỗi kỹ thuật (Technical Errors)
 */
public enum TechnicalAlertCode implements IAlertCode {
    DATABASE_ERROR("500001", "Lỗi cơ sở dữ liệu", AlertType.ERROR),
    SYSTEM_ERROR("500002", "Lỗi hệ thống", AlertType.ERROR),
    KEYCLOAK_ERROR("500003", "Lỗi tích hợp Keycloak", AlertType.ERROR),
    OVERVIEW_REPORT_ERROR("500004", "Lỗi khi tạo báo cáo tổng quan", AlertType.ERROR),
    BUSINESS_TODAY_REPORT_ERROR("500005", "Lỗi khi tạo báo cáo kinh doanh hôm nay", AlertType.ERROR),
    COMPARE_CHART_ERROR("500006", "Lỗi khi tạo biểu đồ so sánh", AlertType.ERROR),
    CONFIG_UPDATE_ERROR("500007", "Lỗi khi cập nhật cấu hình người dùng", AlertType.ERROR),
    MONTHLY_TARGET_ERROR("500008", "Lỗi khi tạo báo cáo chỉ tiêu tháng", AlertType.ERROR),
    ;

    private final AlertCode alertCode;

    TechnicalAlertCode(final String code, final String label, final AlertType alertType) {
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
