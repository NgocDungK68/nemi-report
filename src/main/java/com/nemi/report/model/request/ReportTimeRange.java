package com.nemi.report.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportTimeRange {
    private LocalDateTime from;
    private LocalDateTime to;

    public static ReportTimeRange of(LocalDateTime from, LocalDateTime to) {
        return ReportTimeRange.builder()
                .from(from)
                .to(to)
                .build();
    }

    public static ReportTimeRange today() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return ReportTimeRange.of(startOfDay, endOfDay);
    }

    public static ReportTimeRange thisMonth() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        return ReportTimeRange.of(startOfMonth, endOfMonth);
    }
}
