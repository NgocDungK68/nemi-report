package com.nemi.report.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.YearMonth;

@UtilityClass
public class ReportDateUtils {
    public static int getDaysInMonth() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        return YearMonth.of(currentYear, currentMonth).lengthOfMonth();
    }
}
