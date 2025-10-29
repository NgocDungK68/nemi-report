package com.nemi.report.service;

import com.nemi.report.model.request.overview.MonthlyTargetRequest;
import com.nemi.report.model.request.overview.UpdateMonthlyTargetRequest;
import com.nemi.report.model.response.overview.MonthlyTargetResponse;

public interface MonthlyTargetService {
    MonthlyTargetResponse getMonthlyTarget(MonthlyTargetRequest request);
    void updateMonthlyTarget(UpdateMonthlyTargetRequest request);
}
