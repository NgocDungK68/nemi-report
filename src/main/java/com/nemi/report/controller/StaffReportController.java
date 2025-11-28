package com.nemi.report.controller;

import com.nemi.annotation.RequirePermission;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.staff.StaffChartRequest;
import com.nemi.report.model.response.staff.StaffChartResponse;
import com.nemi.report.model.response.staff.StaffDailyResponse;
import com.nemi.report.model.response.staff.StaffSummaryResponse;
import com.nemi.report.model.response.staff.StaffsChartResponse;
import com.nemi.report.service.StaffReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("client-api/v1/report/staff")
@RequiredArgsConstructor
public class StaffReportController {

    private final StaffReportService staffReportService;

    @RequirePermission("REPORTING.STAFF_REPORT.VIEW")
    @PostMapping
    public ResponseEntity<StaffSummaryResponse> getStaffSummary(@RequestBody ReportSummaryRequest request) {
        StaffSummaryResponse response = staffReportService.getStaffReportSummary(request);

        return ResponseEntity.ok(response);
    }

    @RequirePermission("REPORTING.STAFF_REPORT.VIEW")
    @PostMapping("/{staffId}")
    public ResponseEntity<StaffDailyResponse> getStaffSummary(
            @PathVariable(name = "staffId") String staffId,
            @RequestBody ReportSummaryRequest request) {
        StaffDailyResponse response = staffReportService.getStaffDaily(staffId, request);
        return ResponseEntity.ok(response);
    }

    @RequirePermission("REPORTING.STAFF_REPORT.VIEW")
    @PostMapping("/staffs-chart")
    public ResponseEntity<StaffsChartResponse> getStaffsChart(@RequestBody StaffChartRequest request) {
        StaffsChartResponse response = staffReportService.getAllStaffsChart(request);
        return ResponseEntity.ok(response);
    }

    @RequirePermission("REPORTING.STAFF_REPORT.VIEW")
    @PostMapping("/staff-chart/{userId}")
    public ResponseEntity<StaffChartResponse> getStaffChart(
            @PathVariable(name = "userId") String userId,
            @RequestBody StaffChartRequest request
    ) {
        StaffChartResponse response = staffReportService.getStaffChart(userId, request);

        return ResponseEntity.ok(response);
    }
}
