package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.ColumnLegend;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.constant.OverviewDataType;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.overview.CompareChartRequest;
import com.nemi.report.model.response.overview.CompareChartResponse;
import com.nemi.report.model.response.overview.ConfigResponse;
import com.nemi.report.service.CompareChartService;
import com.nemi.report.service.ConfigService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ValidationUtils;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompareChartServiceImpl implements CompareChartService {
    private final ReportConfig reportConfig;
    private final OverviewReportServiceImpl overviewReportService;
    private final ConfigService configService;
    private final CurrencyRateService currencyRateService;
    private final ClaimUtil claimUtil;

    @Override
    public CompareChartResponse getCompareChart(CompareChartRequest request) {
        log.info("[CompareChartServiceImpl.getCompareChart] Start calculating compare chart: from={} to={} compareWith={} dataType={} currency={}",
                request.getFrom(), request.getTo(), request.getCompareWith(), request.getDataType(), request.getCurrency());

        ValidationUtils.validateTimeRange(request.getFrom(), request.getTo());

        try {
            int minusDays = request.getCompareWith().getDays();
            CurrencyRates currentCurrencyRates = currencyRateService.getCurrencyRate(
                    claimUtil.getCompanyId(),
                    request.getFrom(),
                    request.getTo(),
                    request.getCurrency()
            );
            CurrencyRates previousCurrencyRates = currencyRateService.getCurrencyRate(
                    claimUtil.getCompanyId(),
                    request.getFrom().minusDays(minusDays),
                    request.getTo().minusDays(minusDays),
                    request.getCurrency()
            );

            ConfigResponse config = configService.getConfig();
            int changePercentScale = reportConfig.getScale().getPercent();
            int stepDays = reportConfig.getCompareChart().getStepDays();
            log.debug("[CompareChartServiceImpl.getCompareChart] Loaded config: {}, Steps days: {}", config, stepDays);

            List<CompareChartResponse.ChartDataPoint> dataList = new ArrayList<>();
            for (LocalDate date = request.getFrom(); !date.isAfter(request.getTo()); date = date.plusDays(stepDays)) {
                LocalDate compareDate = date.minusDays(request.getCompareWith().getDays());
                log.debug("[CompareChartServiceImpl.getCompareChart] Calculating data for date={} compareDate={}", date, compareDate);

                CurrencyRates currentCurrencyRate = currentCurrencyRates.currencyRateIn(date);
                CurrencyRates previousCurrencyRate = previousCurrencyRates.currencyRateIn(date);

                BigDecimal presentValue = getValueByDataType(request.getDataType(), config, currentCurrencyRate);
                BigDecimal previousValue = getValueByDataType(request.getDataType(), config, previousCurrencyRate);
                BigDecimal changePercent = ReportUtils.changePercent(presentValue, previousValue, changePercentScale);

                // TODO: code additions
                CompareChartResponse.ChartDataPoint item = CompareChartResponse.ChartDataPoint.builder()
                        .date(date.format(DateTimeFormatter.ofPattern(reportConfig.getDatePattern())))
                        .presentValue(presentValue)
                        .previousValue(previousValue)
                        .changePercent(changePercent)
                        .additions(null)
                        .build();

                log.debug("[CompareChartServiceImpl.getCompareChart] Data point: date={} presentValue={} previousValue={} changePercent={}",
                        item.getDate(), presentValue, previousValue, changePercent);

                dataList.add(item);
            }

            log.info("[CompareChartServiceImpl.getCompareChart] Successfully calculating compare chart for range {} to {}",
                    request.getFrom(), request.getTo());

            return CompareChartResponse.builder()
                    .data(dataList)
                    .columnLegend(getColumnLegend(request.getDataType(), request.getCurrency()))
                    .build();
        } catch (Exception e) {
            log.error("[CompareChartServiceImpl.getCompareChart] Failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.COMPARE_CHART_ERROR));
        }
    }

    private BigDecimal getValueByDataType(OverviewDataType dataType, ConfigResponse config, CurrencyRates currencyRates) {
        log.debug("[CompareChartServiceImpl.getValueByDataType] Getting {} value for date={}",
                dataType, currencyRates.getFrom());

        // TODO: Code AD_COST, AD_COST_PER_REVENUE
        return switch (dataType) {
            case REVENUE ->
                    overviewReportService.getOrderSummary(OrderStatus.getTotalOrdersStatus(), currencyRates).getRevenue();
            case AD_COST, AD_COST_PER_REVENUE -> BigDecimal.ZERO;
            case RETURNED_ORDER ->
                    overviewReportService.getOrderSummary(config.getReturnOrderWhen().getOrderStatus(), currencyRates).getRevenue();
            case PROFIT -> overviewReportService.getProfit(currencyRates);
        };
    }

    private String getColumnLegend(OverviewDataType dataType, Currency currency) {
        return switch (dataType) {
            case AD_COST_PER_REVENUE -> ColumnLegend.PERCENT.getCode();
            case REVENUE, AD_COST, PROFIT, RETURNED_ORDER -> currency.getCode();
        };
    }
}
