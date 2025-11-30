package com.nemi.report.service;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.model.pojo.MonthlyReport;
import com.nemi.report.model.request.overview.BusinessTodayRequest;
import com.nemi.report.model.request.overview.CompareChartRequest;
import com.nemi.report.model.request.overview.OverviewReportRequest;
import com.nemi.report.model.response.overview.BusinessTodayResponse;
import com.nemi.report.model.response.overview.CompareChartResponse;
import com.nemi.report.model.response.overview.OverviewReportResponse;

public interface OverviewReportService {
    OverviewReportResponse getOverviewReport(OverviewReportRequest request);
    CompareChartResponse getCompareChart(CompareChartRequest request);
    BusinessTodayResponse getBusinessToday(BusinessTodayRequest request);

    MonthlyReport getMonthlyReport(CurrencyCodeEnum currencyCode);
}
