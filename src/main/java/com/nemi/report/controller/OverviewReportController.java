package com.nemi.report.controller;

import com.nemi.report.constant.CompareWithType;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OverviewDataType;
import com.nemi.report.model.request.overview.*;
import com.nemi.report.model.response.overview.*;
import com.nemi.report.service.BusinessTodayService;
import com.nemi.report.service.CompareChartService;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.MonthlyTargetService;
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
    private final CompareChartService compareChartService;
    private final BusinessTodayService businessTodayService;
    private final MonthlyTargetService monthlyTargetService;
    private final ConfigService configService;

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

        CompareChartRequest request = new CompareChartRequest();
        request.setFrom(from);
        request.setTo(to);
        request.setCompareWith(compareWith);
        request.setCurrency(currency);
        request.setDataType(dataType);

        CompareChartResponse response = compareChartService.getCompareChart(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/business-today")
    public ResponseEntity<BusinessTodayResponse> getBusinessToday(
            @RequestParam("currency") Currency currency) {

        BusinessTodayRequest request = new BusinessTodayRequest();
        request.setCurrency(currency);
        BusinessTodayResponse response = businessTodayService.getBusinessToday(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-target")
    public ResponseEntity<MonthlyTargetResponse> getMonthlyTarget(
            @RequestParam("currency") Currency currency) {

        MonthlyTargetRequest request = new MonthlyTargetRequest();
        request.setCurrency(currency);
        MonthlyTargetResponse response = monthlyTargetService.getMonthlyTarget(request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/monthly-target")
    public ResponseEntity<String> updateMonthlyTarget(
            @Valid @RequestBody UpdateMonthlyTargetRequest request) {

        monthlyTargetService.updateMonthlyTarget(request);
        return ResponseEntity.ok("200");
    }

    @GetMapping("/config")
    public ResponseEntity<ConfigResponse> getConfig() {

        ConfigResponse response = configService.getConfig();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/config")
    public ResponseEntity<ConfigResponse> updateConfig(
            @RequestBody ConfigRequest request) {

        ConfigResponse response = configService.updateConfig(request);
        return ResponseEntity.ok(response);
    }
}
