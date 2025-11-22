package com.nemi.report.util;

import org.apache.commons.lang3.ObjectUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    public static int getDaysInMonth() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        return YearMonth.of(currentYear, currentMonth).lengthOfMonth();
    }

    /**
     * Tính tổng số ngày giữa startDate và endDate, bao gồm cả 2 ngày.
     */
    public static int daysBetweenInclusive(LocalDate startDate, LocalDate endDate) {
        if (ObjectUtils.isEmpty(startDate) || ObjectUtils.isEmpty(endDate)) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public static String convertInstantToString(Instant instant) {
        if (instant != null && instant.getEpochSecond() >= 0) {
            return com.nemi.util.DateUtils.instantToTimeString(instant);
        }
        return null;
    }
}
