package com.nemi.report.service;

import com.nemi.report.model.request.overview.OverviewReportRequest;
import com.nemi.report.model.response.overview.OverviewReportResponse;

public interface OverviewReportService {
    OverviewReportResponse getOverviewReport(OverviewReportRequest request);
}
