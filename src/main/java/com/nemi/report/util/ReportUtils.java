package com.nemi.report.util;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ReportUtils {
    /**
     * Công thức:
     *     changePercent = ((current - previous) / previous) * 100
     * @param current  Giá trị hiện tại (kỳ đang xét)
     * @param previous Giá trị kỳ trước (kỳ so sánh)
     * @return Tỉ lệ thay đổi (đơn vị: phần trăm, ví dụ 16.02 nghĩa là tăng 16.02%)
     */
    public static BigDecimal changePercent(BigDecimal current, BigDecimal previous, int scale) {
        if (ObjectUtils.isEmpty(previous) || previous.compareTo(BigDecimal.ZERO) == 0) {
            // Nếu kỳ trước = 0 và kỳ này có giá trị => tăng 100%
            if (ObjectUtils.isNotEmpty(current) && current.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100);
            }
            // Nếu cả hai = 0 hoặc current null => không thay đổi
            return BigDecimal.ZERO;
        }

        // Nếu kỳ trước khác 0 => tính theo công thức
        BigDecimal diff = current.subtract(previous);
        BigDecimal percent = diff
                .divide(previous, scale + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percent.setScale(scale, RoundingMode.HALF_UP);  // Làm tròn 2 chữ số sau dấu phẩy
    }

    public static BigDecimal calculatePercentage(BigDecimal value, BigDecimal total, int scale) {
        if (ObjectUtils.isEmpty(value) || ObjectUtils.isEmpty(total) || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return value
                .divide(total, scale + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(List<? extends Number> listNumber) {
        return listNumber.stream()
                .filter(Objects::nonNull)
                .map(num -> new BigDecimal(num.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal convertToBigDecimal(Object object) {
        BigDecimal value = BigDecimal.ZERO;

        if (object instanceof Number number) {
            value = new BigDecimal(number.toString());
        }

        return value;
    }

    public static int calculateTotalPages(int totalElements, int size) {
        return (int) Math.ceil(totalElements / (double) size);
    }
}
