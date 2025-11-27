package com.nemi.report.model.request.staff;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.constant.Limit;
import com.nemi.report.constant.ReportChartType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StaffChartRequest {
    @NotNull
    private CurrencyCodeEnum currency; // VND/USD

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull
    private ReportChartType chartData;

    private boolean splitByDate = false;

    private Limit limit = Limit.TOP5;
}
