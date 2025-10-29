package com.nemi.report.service;

import com.nemi.report.model.request.overview.CompareChartRequest;
import com.nemi.report.model.response.overview.CompareChartResponse;

public interface CompareChartService {
    CompareChartResponse getCompareChart(CompareChartRequest request);
}
