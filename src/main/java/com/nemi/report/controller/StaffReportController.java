package com.nemi.report.controller;

import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.staff.StaffChartResponse;
import com.nemi.report.model.response.staff.StaffDailyResponse;
import com.nemi.report.model.response.staff.StaffSummaryResponse;
import com.nemi.report.model.response.staff.StaffsChartResponse;
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
    @PostMapping("/summary")
    public ResponseEntity<StaffSummaryResponse> getStaffSummary(@RequestBody ReportSummaryRequest request) {
        // TODO: Implement service call
        // StaffSummaryResponse response = staffSummaryService.getStaffSummary(request);

        // Mock
        StaffSummaryResponse response = new StaffSummaryResponse();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{staffId}")
    public ResponseEntity<StaffDailyResponse> getStaffSummary(
            @PathVariable(name = "staffId") String staffId,
            @RequestBody ReportSummaryRequest request
    ) {
        // TODO: Implement service call
        // StaffDailyResponse response = staffSummaryService.getStaffDaily(staffId, request);

        // Mock
        StaffDailyResponse response = new StaffDailyResponse();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staffs-chart")
    public ResponseEntity<StaffsChartResponse> getStaffsChart(@RequestBody ReportChartRequest request) {
        // TODO: Implement service call
        // StaffsChartResponse response = staffChartService.getStaffsChart(request);

        // Mock
        StaffsChartResponse response = new StaffsChartResponse();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff-chart/{staffId}")
    public ResponseEntity<StaffChartResponse> getStaffChart(
            @PathVariable(name = "staffId") String staffId,
            @RequestBody ReportChartRequest request
    ) {
        // TODO: Implement service call
        // StaffChartResponse response = staffChartService.getStaffChart(staffId, request);

        // Mock
        StaffChartResponse response = new StaffChartResponse();
        return ResponseEntity.ok(response);
    }
}
