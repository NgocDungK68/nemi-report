package com.nemi.report.model.request.marketing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.constant.MarketingChartType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MarketingChartRequest {
    @NotNull
    private CurrencyCodeEnum currency; // VND/USD

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull
    private MarketingChartType chartData;
}
