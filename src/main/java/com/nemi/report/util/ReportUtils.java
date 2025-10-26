package com.nemi.report.util;

import com.nemi.report.configuration.ReportConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class ReportUtils {
    private final ReportConfig reportConfig;

    /**
     * Công thức:
     *     changePercent = ((current - previous) / previous) * 100
     * @param current  Giá trị hiện tại (kỳ đang xét)
     * @param previous Giá trị kỳ trước (kỳ so sánh)
     * @return Tỉ lệ thay đổi (đơn vị: phần trăm, ví dụ 16.02 nghĩa là tăng 16.02%)
     */
    public BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
        int scale = reportConfig.getScale().getChangePercent();
        if (ObjectUtils.isEmpty(previous) || previous.compareTo(BigDecimal.ZERO) == 0) {
            // Nếu kỳ trước = 0 và kỳ này có giá trị => tăng 100%
            if (ObjectUtils.isNotEmpty(current) && current.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100.00).setScale(scale, RoundingMode.HALF_UP);
            }
            // Nếu cả hai = 0 hoặc current null => không thay đổi
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }

        // Nếu kỳ trước khác 0 => tính theo công thức
        BigDecimal diff = current.subtract(previous);
        BigDecimal percent = diff
                .divide(previous, scale + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percent.setScale(scale, RoundingMode.HALF_UP);  // Làm tròn 2 chữ số sau dấu phẩy
    }
}
