package com.nemi.report.controller;

import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.service.ProductChartService;
import com.nemi.report.service.ProductSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client-api/v1/report/product")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductSummaryService productSummaryService;

    //    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    private final ProductChartService productChartService;

    @PostMapping
    public ResponseEntity<ProductSummaryResponse> getProductSummary(@Valid @RequestBody ReportSummaryRequest request) {

        ProductSummaryResponse response = productSummaryService.getProductSummary(request);
        return ResponseEntity.ok(response);
    }

//    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/{productId}")
    public ResponseEntity<ProductDailyResponse> getProductSummary(
            @PathVariable(name = "productId") String productId,
            @RequestBody ReportSummaryRequest request
    ) {
        ProductDailyResponse response = productSummaryService.getProductDaily(request, productId);
        return ResponseEntity.ok(response);
    }

//    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/products-chart")
    public ResponseEntity<ProductsChartResponse> getProductsChart(@RequestBody ReportChartRequest request) {
        ProductsChartResponse response = productChartService.getProductsChart(request);
        return ResponseEntity.ok(response);
    }

//    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/product-chart/{productId}")
    public ResponseEntity<ProductChartResponse> getProductChart(
            @PathVariable(name = "productId") String productId,
            @RequestBody ReportChartRequest request
    ) {
        ProductChartResponse response = productChartService.getProductChartByProductId(productId, request);
        return ResponseEntity.ok(response);
    }
}
