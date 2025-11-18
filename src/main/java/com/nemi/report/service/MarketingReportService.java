package com.nemi.report.service;

import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.marketing.MarketingChartRequest;
import com.nemi.report.model.response.marketing.MarketingChartResponse;
import com.nemi.report.model.response.marketing.MarketingReportResponse;

public interface MarketingReportService {

    MarketingReportResponse getMarketingReportSummary(ReportSummaryRequest request);

    MarketingChartResponse getMarketingChart(MarketingChartRequest request);
}
