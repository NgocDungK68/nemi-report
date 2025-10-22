package com.nemi.report.service.impl;

import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.ColumnLegend;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.constant.OverviewDataType;
import com.nemi.report.model.request.CompareChartRequest;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.response.CompareChartResponse;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.CompareChartService;
import com.nemi.report.util.ReportUtil;
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
    private final ReportUtil reportUtil;
    private final ReportConfig reportConfig;
    private final OrderRepository orderRepository;
    private final OverviewReportServiceImpl overviewReportService;

    @Override
    public CompareChartResponse getCompareChart(CompareChartRequest request) {
        List<CompareChartResponse.ChartDataPoint> dataList = new ArrayList<>();

        for (LocalDate date = request.getFrom(); !date.isAfter(request.getTo()); date = date.plusDays(1)) {
            LocalDate compareDate = date.minusDays(request.getCompareWith().getDays());

            BigDecimal presentValue = getValueByDataType(date, request.getDataType());
            BigDecimal previousValue = getValueByDataType(compareDate, request.getDataType());
            BigDecimal changePercent = reportUtil.changePercent(presentValue, previousValue);

            CompareChartResponse.ChartDataPoint item = CompareChartResponse.ChartDataPoint.builder()
                    .date(date.format(DateTimeFormatter.ofPattern(reportConfig.getDatePattern())))
                    .presentValue(presentValue)
                    .previousValue(previousValue)
                    .changePercent(changePercent)
                    .additions(null)         // xu ly sau
                    .build();

            dataList.add(item);
        }

        return CompareChartResponse.builder()
                .data(dataList)
                .columnLegend(getColumnLegend(request.getDataType(), request.getCurrency()))
                .build();
    }

    private BigDecimal getValueByDataType(LocalDate date, OverviewDataType dataType) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        ReportTimeRange timeRange = ReportTimeRange.of(from, to);

        return switch (dataType) {
            case REVENUE -> overviewReportService.getOrderRevenue(orderRepository.findByUpdatedAtBetween(from, to));
            case AD_COST, AD_COST_PER_REVENUE -> BigDecimal.ZERO;
            case RETURNED_ORDER -> {
                List<String> returnStatus = OrderStatus.getReturnedOrdersStatus();
                yield overviewReportService.getOrderRevenue(orderRepository.findByStatusInAndUpdatedAtBetween(returnStatus, from, to));
            }
            case PROFIT -> overviewReportService.getProfit(timeRange);
        };
    }

    private String getColumnLegend(OverviewDataType dataType, Currency currency) {
        return switch (dataType) {
            case AD_COST_PER_REVENUE -> ColumnLegend.PERCENT.getCode();
            case REVENUE, AD_COST, PROFIT, RETURNED_ORDER -> currency.name();
        };
    }
}
