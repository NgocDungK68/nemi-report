package com.nemi.report.controller;

import com.nemi.annotation.RequirePermission;
import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.service.ProductChartService;
import com.nemi.report.service.ProductSumaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client-api/v1/report/product")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductSumaryService productSumaryService;

    //    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    private final ProductChartService productChartService;

    @PostMapping("/summary")
    public ResponseEntity<ProductSummaryResponse> getProductSummary(@Valid @RequestBody ProductSummaryRequest request) {
        // TODO: Implement service call
        // ProductSummaryResponse response = productSummaryService.getProductSummary(request);

        // Mock
        ProductSummaryResponse response = productSumaryService.getProductSumary(request, null);
        return ResponseEntity.ok(response);
    }

//    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("summary/{productId}")
    public ResponseEntity<ProductSummaryResponse> getProductSummary(
            @PathVariable(name = "productId") String productId,
            @RequestBody ProductSummaryRequest request
    ) {
        // TODO: Implement service call
        // ProductDailyResponse response = productSummaryService.getProductDaily(productId, request);

        // Mock
        ProductSummaryResponse response = productSumaryService.getProductSumary(request, productId);
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
