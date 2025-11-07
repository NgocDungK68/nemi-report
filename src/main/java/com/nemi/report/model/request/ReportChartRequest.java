package com.nemi.report.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.report.constant.ChartData;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.Limit;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportChartRequest {
    @NotNull
    private ChartData chartData;

    @NotNull
    private Currency currency;

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate endDate;

    @NotNull
    private boolean splitByDate;

    @NotNull
    private Limit limit;
}
