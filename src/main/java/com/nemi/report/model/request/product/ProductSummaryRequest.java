package com.nemi.report.model.request.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.FilterType;
import com.nemi.report.constant.OrderSort;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProductSummaryRequest {
    @NotNull
    private Currency currency; // VND/USD

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

    private List<ColumnConfig> columns;

    private List<FilterConfig> filters;

    @Data
    public static class ColumnConfig {
        @NotNull
        private String code;

        @NotNull
        private OrderSort order;
    }

    @Data
    public static class FilterConfig {
        @NotNull
        private String code;

        @NotNull
        private FilterType type;

        private List<String> value;
    }
}
