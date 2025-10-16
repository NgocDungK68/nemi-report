package com.nemi.report.service;

import com.nemi.report.model.request.OverviewReportRequest;
import com.nemi.report.model.response.OverviewReportResponse;

public interface OverviewReportService {
    OverviewReportResponse getOverviewReport(OverviewReportRequest request);
}
