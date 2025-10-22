package com.nemi.report.service.impl;

import com.nemi.report.model.request.MonthlyTargetRequest;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.request.UpdateMonthlyTargetRequest;
import com.nemi.report.model.response.MonthlyTargetResponse;
import com.nemi.report.service.MonthlyTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlyTargetServiceImpl implements MonthlyTargetService {
    private final BusinessTodayServiceImpl businessTodayService;

    @Override
    public MonthlyTargetResponse getMonthlyTarget(MonthlyTargetRequest request) {
        ReportTimeRange thisMonth = ReportTimeRange.thisMonth();
        return MonthlyTargetResponse.builder()
                .todayRevenue(businessTodayService.getRevenueToday())
                .build();
    }

    @Override
    public void updateMonthlyTarget(UpdateMonthlyTargetRequest request) {

    }
}
