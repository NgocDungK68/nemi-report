package com.nemi.report.model.request.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.report.constant.Currency;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProductSummaryRequest {
    @NotNull
    private Currency currency; // VND/USD

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotNull
    private Integer page;

    @NotNull
    private Integer size;

    private List<ColumnRequest> columns;

    private List<FilterRequest> filters;

}

