package com.nemi.report.service;

import com.nemi.report.model.request.CompareChartRequest;
import com.nemi.report.model.response.CompareChartResponse;

public interface CompareChartService {
    CompareChartResponse getCompareChart(CompareChartRequest request);
}
