package com.nemi.report.controller;

import com.nemi.report.model.request.MarketingSummaryRequest;
import com.nemi.report.model.response.MarketingSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("client-api/v1/report/marketing")
@RequiredArgsConstructor
public class MarketingReportController {

    @PostMapping("/summary")
    public ResponseEntity<MarketingSummaryResponse> getMarketingSummary(
            @Valid @RequestBody MarketingSummaryRequest request) {

        // TODO: Implement service call
        // MarketingSummaryResponse response = marketingSummaryService.getMarketingSummary(request);

        // Temporary mock response
        MarketingSummaryResponse response = new MarketingSummaryResponse();

        return ResponseEntity.ok(response);
    }
}
