package com.nemi.report.service;

import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.staff.StaffChartRequest;
import com.nemi.report.model.response.staff.StaffChartResponse;
import com.nemi.report.model.response.staff.StaffDailyResponse;
import com.nemi.report.model.response.staff.StaffSummaryResponse;
import com.nemi.report.model.response.staff.StaffsChartResponse;

public interface StaffReportService {

    StaffSummaryResponse getStaffReportSummary(ReportSummaryRequest request);

    StaffDailyResponse getStaffDaily(String userId, ReportSummaryRequest request);

    StaffsChartResponse getAllStaffsChart(StaffChartRequest request);

    StaffChartResponse getStaffChart(String userId, StaffChartRequest request);
}
