package com.nemi.report.service;

import com.nemi.report.entity.OrderEntity;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.overview.OverviewReportRequest;
import com.nemi.report.model.response.overview.OverviewReportResponse;
import com.nemi.report.model.response.overview.RevenueSummary;

import java.math.BigDecimal;
import java.util.List;

public interface OverviewReportService {
    OverviewReportResponse getOverviewReport(OverviewReportRequest request);
    RevenueSummary getOrderSummary(List<String> orderStatus, CurrencyRates currencyRates);
    BigDecimal getOrderRevenue(List<OrderEntity> orders);
    RevenueSummary getAdsSummary(CurrencyRates currencyRates);
    BigDecimal getProfit(CurrencyRates currencyRates);
}
