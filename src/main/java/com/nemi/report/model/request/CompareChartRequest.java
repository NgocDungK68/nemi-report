package com.nemi.report.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.report.constant.CompareWithType;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OverviewDataType;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompareChartRequest {

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate from;

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate to;

    private List<CompareWithType> compareWith; // L7D, L1M, L2M, L3M, LY

    @NotNull
    private Currency currency; // VND/USD

    @NotNull
    private OverviewDataType dataType; // REVENUE, AD_COST, AD_COST_PER_REVENUE, RETURNED_ORDER, PROFIT
}
