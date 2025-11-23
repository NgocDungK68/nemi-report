package com.nemi.report.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.constant.ChartData;
import com.nemi.report.constant.Limit;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportChartRequest {
    @NotNull
    private ChartData chartData;

    @NotNull
    private CurrencyCodeEnum currency;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull
    private boolean splitByDate;

    @NotNull
    private Limit limit;
}
