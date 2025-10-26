package com.nemi.report.service.impl;

import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.ColumnLegend;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.constant.OverviewDataType;
import com.nemi.report.model.request.CompareChartRequest;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.response.CompareChartResponse;
import com.nemi.report.model.response.ConfigResponse;
import com.nemi.report.service.CompareChartService;
import com.nemi.report.service.ConfigService;
import com.nemi.report.util.ReportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompareChartServiceImpl implements CompareChartService {
    private final ReportUtils reportUtils;
    private final ReportConfig reportConfig;
    private final OverviewReportServiceImpl overviewReportService;
    private final ConfigService configService;

    @Override
    public CompareChartResponse getCompareChart(CompareChartRequest request) {
        List<CompareChartResponse.ChartDataPoint> dataList = new ArrayList<>();
        ConfigResponse config = configService.getConfig();

        for (LocalDate date = request.getFrom(); !date.isAfter(request.getTo()); date = date.plusDays(reportConfig.getCompareChart().getStepDays())) {
            LocalDate compareDate = date.minusDays(request.getCompareWith().getDays());

            BigDecimal presentValue = getValueByDataType(date, request.getDataType(), config);
            BigDecimal previousValue = getValueByDataType(compareDate, request.getDataType(), config);
            BigDecimal changePercent = reportUtils.changePercent(presentValue, previousValue);

            // TODO: code additions
            CompareChartResponse.ChartDataPoint item = CompareChartResponse.ChartDataPoint.builder()
                    .date(date.format(DateTimeFormatter.ofPattern(reportConfig.getDatePattern())))
                    .presentValue(presentValue)
                    .previousValue(previousValue)
                    .changePercent(changePercent)
                    .additions(null)
                    .build();

            dataList.add(item);
        }

        return CompareChartResponse.builder()
                .data(dataList)
                .columnLegend(getColumnLegend(request.getDataType(), request.getCurrency()))
                .build();
    }

    private BigDecimal getValueByDataType(LocalDate date, OverviewDataType dataType, ConfigResponse config) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        ReportTimeRange timeRange = ReportTimeRange.of(from, to);

        return switch (dataType) {
            case REVENUE ->
                    overviewReportService.getOrderSummary(timeRange, OrderStatus.getTotalOrdersStatus()).getRevenue();
            case AD_COST, AD_COST_PER_REVENUE ->
                    BigDecimal.ZERO;
            case RETURNED_ORDER ->
                    overviewReportService.getOrderSummary(timeRange, config.getReturnOrderWhen().getOrderStatus()).getRevenue();
            case PROFIT ->
                    overviewReportService.getProfit(timeRange);
        };
    }

    private String getColumnLegend(OverviewDataType dataType, Currency currency) {
        // TODO: code column legend
        return switch (dataType) {
            case AD_COST_PER_REVENUE -> ColumnLegend.PERCENT.getCode();
            case REVENUE, AD_COST, PROFIT, RETURNED_ORDER -> null;
        };
    }
}
