package com.nemi.report.controller;

import com.nemi.annotation.RequirePermission;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.marketing.MarketingChartRequest;
import com.nemi.report.model.response.marketing.MarketingChartResponse;
import com.nemi.report.model.response.marketing.MarketingReportResponse;
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

    @RequirePermission("REPORTING.MARKETING_REPORT.VIEW")
    @PostMapping("/summary")
    public ResponseEntity<MarketingReportResponse> getMarketingSummary(
            @Valid @RequestBody ReportSummaryRequest request) {
        return ResponseEntity.ok(marketingReportService.getMarketingReportSummary(request));
    }

    @RequirePermission("REPORTING.MARKETING_REPORT.VIEW")
    @PostMapping("/chart")
    public ResponseEntity<MarketingChartResponse> getMarketingChart(
            @Valid @RequestBody MarketingChartRequest request) {
        return ResponseEntity.ok(marketingReportService.getMarketingChart(request));
    }
}
