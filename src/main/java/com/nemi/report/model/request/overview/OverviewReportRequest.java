package com.nemi.report.model.request.overview;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.constant.CompareWithType;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class OverviewReportRequest {

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate from;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate to;

    private CompareWithType compareWith; // L7D, L1M, L2M, L3M, LY

    @NotNull
    private CurrencyCodeEnum currency; //
}
