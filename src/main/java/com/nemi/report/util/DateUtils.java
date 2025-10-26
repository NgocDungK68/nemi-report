package com.nemi.report.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtils {
    public static int getDaysInMonth() {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        return YearMonth.of(currentYear, currentMonth).lengthOfMonth();
    }
}
