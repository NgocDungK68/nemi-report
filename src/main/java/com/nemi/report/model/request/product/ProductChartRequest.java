package com.nemi.report.model.request.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.constant.Limit;
import com.nemi.report.constant.ReportChartType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProductChartRequest {
    @NotNull
    private ReportChartType chartData;

    @NotNull
    private CurrencyCodeEnum currency;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull
    private boolean splitByDate = false;

    @NotNull
    private Limit limit = Limit.TOP5;
}
