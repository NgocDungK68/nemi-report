package com.nemi.report.service;

import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.staff.StaffDailyResponse;
import com.nemi.report.model.response.staff.StaffSummaryResponse;

public interface StaffReportService {

    StaffSummaryResponse getStaffReportSummary(ReportSummaryRequest request);

    StaffDailyResponse getStaffDailyResponse(String userId, ReportSummaryRequest request);
}
