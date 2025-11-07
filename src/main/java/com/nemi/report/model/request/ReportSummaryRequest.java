package com.nemi.report.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.constant.CurrencyCodeEnum;
import jakarta.validation.constraints.Min;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReportSummaryRequest {

    @NotNull
    private CurrencyCodeEnum currency; // VND/USD

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate endDate;

    @NotNull
    private Integer page;

    @NotNull
    private Integer size;

    @NotNull
    @Min(0)
    private List<ColumnRequest> columns;

    private List<FilterRequest> filters;
}
