package com.nemi.report.controller;

import com.nemi.annotation.RequirePermission;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.ReportSummaryResponse;
import com.nemi.report.service.MarketingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("client-api/v1/report/marketing")
@RequiredArgsConstructor
public class MarketingReportController {

    private final MarketingReportService marketingReportService;

//    @RequirePermission("REPORTING.MARKETING_REPORT.VIEW")
    @PostMapping("/summary")
    public ResponseEntity<ReportSummaryResponse> getMarketingSummary(
            @Valid @RequestBody ReportSummaryRequest request) {
        return ResponseEntity.ok(marketingReportService.getMarketingReportSummary(request));
    }
}
