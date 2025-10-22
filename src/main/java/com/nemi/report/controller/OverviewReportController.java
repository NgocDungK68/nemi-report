package com.nemi.report.controller;

import com.nemi.report.constant.CompareWithType;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OverviewDataType;
import com.nemi.report.model.request.*;
import com.nemi.report.model.response.*;
import com.nemi.report.service.impl.OverviewReportServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("client-api/v1/report/overview")
@RequiredArgsConstructor
public class OverviewReportController {
    private final OverviewReportServiceImpl overviewReportService;

    @GetMapping("/revenue")
    public ResponseEntity<OverviewReportResponse> getOverviewReport(
            @RequestParam("from") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to,
            @RequestParam(value = "compareWith", required = false) CompareWithType compareWith,
            @RequestParam("currency") Currency currency) {

        OverviewReportRequest request = new OverviewReportRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setCompareWith(compareWith);
        request.setCurrency(currency);

        OverviewReportResponse response = overviewReportService.getOverviewReport(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/compare-chart")
    public ResponseEntity<CompareChartResponse> getCompareChart(
            @RequestParam("from") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate from,
            @RequestParam("to") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate to,
            @RequestParam(value = "compareWith", required = false) CompareWithType compareWith,
            @RequestParam("currency") Currency currency,
            @RequestParam("dataType") OverviewDataType dataType) {

        // Create request object from parameters
        CompareChartRequest request = new CompareChartRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setCompareWith(compareWith);
        request.setCurrency(currency);
        request.setDataType(dataType);

        // TODO: Implement service call
        // CompareChartResponse response = compareChartService.getCompareChart(request);

        // Temporary mock response
        CompareChartResponse response = new CompareChartResponse();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/business-today")
    public ResponseEntity<BusinessTodayResponse> getBusinessToday(
            @RequestParam("currency") Currency currency) {

        // Create request object from parameters
        BusinessTodayRequest request = new BusinessTodayRequest();
        request.setCurrency(currency);

        // TODO: Implement service call
        // BusinessTodayResponse response = businessTodayService.getBusinessToday(request);

        // Temporary mock response
        BusinessTodayResponse response = new BusinessTodayResponse();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-target")
    public ResponseEntity<MonthlyTargetResponse> getMonthlyTarget(
            @RequestParam("currency") Currency currency) {

        // Create request object from parameters
        MonthlyTargetRequest request = new MonthlyTargetRequest();
        request.setCurrency(currency);

        // TODO: Implement service call
        // MonthlyTargetResponse response = monthlyTargetService.getMonthlyTarget(request);

        // Temporary mock response
        MonthlyTargetResponse response = new MonthlyTargetResponse();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/monthly-target")
    public ResponseEntity<String> updateMonthlyTarget(
            @Valid @RequestBody UpdateMonthlyTargetRequest request) {

        // TODO: Implement service call
        // monthlyTargetService.updateMonthlyTarget(request);

        return ResponseEntity.ok("200");
    }

    @GetMapping("/config")
    public ResponseEntity<ConfigResponse> getConfig() {

        // TODO: Implement service call
        // ConfigResponse response = configService.getConfig();

//        // Temporary mock response with example values from API spec
//        ConfigResponse response = new ConfigResponse();
//        response.setConfirmOrderWhen(Arrays.asList(
//                "update_status_to_new",
//                "update_status_to_confirm",
//                "move_to_transporter",
//                "reconciled"
//        ));
//        response.setReturnOrderWhen(Arrays.asList(
//                "return",
//                "returned"
//        ));

        return ResponseEntity.ok(null);
    }

    @PutMapping("/config")
    public ResponseEntity<ConfigResponse> updateConfig(
            @RequestBody ConfigResponse request) {

        // TODO: Implement service call
        // ConfigResponse response = configService.updateConfig(request);

        // For now, return the same data that was sent
        return ResponseEntity.ok(request);
    }
}
