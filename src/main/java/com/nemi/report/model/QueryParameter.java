package com.nemi.report.model;

import com.nemi.report.constant.FilterType;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.request.marketing.MarketingSummaryRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryParameter {
    private ColumnConfig column;
    private FilterType filterType;
    private List<String> value;
}
