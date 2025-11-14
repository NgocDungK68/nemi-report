package com.nemi.report.service;

import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.ReportSummaryResponse;

public interface MarketingReportService {

    ReportSummaryResponse getMarketingReportSummary(ReportSummaryRequest request);
}
