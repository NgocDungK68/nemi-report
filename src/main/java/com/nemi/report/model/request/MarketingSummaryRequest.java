package com.nemi.report.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nemi.report.constant.Currency;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class MarketingSummaryRequest {

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
        private String order; // ASC/DESC
    }

    @Data
    public static class FilterConfig {
        @NotNull
        private String code;

        @NotNull
        private String type; // enum type

        private List<String> value;
    }
}
