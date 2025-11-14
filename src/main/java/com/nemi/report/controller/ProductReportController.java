package com.nemi.report.controller;

import com.nemi.annotation.RequirePermission;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.service.ProductSumaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client-api/v1/report/product")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductSumaryService productSumaryService;

//    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/summary")
    public ResponseEntity<ProductSummaryResponse> getProductSummary(@Valid @RequestBody ProductSummaryRequest request) {
        // TODO: Implement service call
        // ProductSummaryResponse response = productSummaryService.getProductSummary(request);

        // Mock
        ProductSummaryResponse response = productSumaryService.getProductSumary(request,null);
        return ResponseEntity.ok(response);
    }

    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/{productId}")
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

    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/products-chart")
    public ResponseEntity<ProductsChartResponse> getProductsChart(@RequestBody ReportChartRequest request) {
        // TODO: Implement service call
        // ProductsChartResponse response = productChartService.getProductsChart(request);

        // Mock
        ProductsChartResponse response = new ProductsChartResponse();
        return ResponseEntity.ok(response);
    }

    @RequirePermission("REPORTING.PRODUCT_REPORT.VIEW")
    @PostMapping("/product-chart/{productId}")
    public ResponseEntity<ProductChartResponse> getProductChart(
            @PathVariable(name = "productId") String productId,
            @RequestBody ReportChartRequest request
    ) {
        // TODO: Implement service call
        // ProductChartResponse response = productChartService.getProductChart(productId, request);

        // Mock
        ProductChartResponse response = new ProductChartResponse();
        return ResponseEntity.ok(response);
    }
}
