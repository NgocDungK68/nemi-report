package com.nemi.report.util;

import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;

@UtilityClass
public class ReportValidator {

    public static List<ColumnRequest> getValidColumn(List<ColumnRequest> columnRequest, List<ColumnConfig> columnConfig) {
        return columnRequest.stream()
                .filter(order -> {
                    ColumnConfig column = getColumnByCode(columnConfig, order.getCode());
                    return Objects.nonNull(column);
                }).toList();
    }

    public static List<FilterRequest> getValidFilters(List<FilterRequest> filters, List<ColumnConfig> columns) {
        return filters.stream()
                .filter(filter -> {
                    ColumnConfig column = getColumnByCode(columns, filter.getCode());
                    return Objects.nonNull(column) && column.getHasFilter();
                }).toList();
    }

    public static List<ColumnRequest> getValidOrders(List<ColumnRequest> orders, List<ColumnConfig> columns) {
        return orders.stream()
                .filter(order -> {
                    ColumnConfig column = getColumnByCode(columns, order.getCode());
                    return Objects.nonNull(column) && column.getHasOrder();
                }).toList();
    }

    private ColumnConfig getColumnByCode(List<ColumnConfig> columns, String code) {
        return columns.stream().filter(col -> col.getCode().equals(code)).findFirst().orElse(null);
    }
}
