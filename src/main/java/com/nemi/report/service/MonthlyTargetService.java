package com.nemi.report.service;

import com.nemi.report.model.request.MonthlyTargetRequest;
import com.nemi.report.model.request.UpdateMonthlyTargetRequest;
import com.nemi.report.model.response.MonthlyTargetResponse;

public interface MonthlyTargetService {
    MonthlyTargetResponse getMonthlyTarget(MonthlyTargetRequest request);
    void updateMonthlyTarget(UpdateMonthlyTargetRequest request);
}
