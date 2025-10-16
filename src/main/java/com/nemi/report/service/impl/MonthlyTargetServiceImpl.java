package com.nemi.report.service.impl;

import com.nemi.report.model.request.MonthlyTargetRequest;
import com.nemi.report.model.request.UpdateMonthlyTargetRequest;
import com.nemi.report.model.response.MonthlyTargetResponse;
import com.nemi.report.service.MonthlyTargetService;
import org.springframework.stereotype.Service;

@Service
public class MonthlyTargetServiceImpl implements MonthlyTargetService {
    @Override
    public MonthlyTargetResponse getMonthlyTarget(MonthlyTargetRequest request) {
        return null;
    }

    @Override
    public void updateMonthlyTarget(UpdateMonthlyTargetRequest request) {

    }
}
