package com.nemi.report.controller;

import com.nemi.report.model.request.product.ProductChartRequest;
import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("client-api/v1/report/product")
@RequiredArgsConstructor
public class ProductReportController {
    @PostMapping("/summary")
    public ResponseEntity<ProductSummaryResponse> getProductSummary(@RequestBody ProductSummaryRequest request) {
        // TODO: Implement service call
        // ProductSummaryResponse response = productSummaryService.getProductSummary(request);

        // Mock
        ProductSummaryResponse response = new ProductSummaryResponse();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ProductDailyResponse> getProductSummary(
            @PathVariable(name = "productId") String productId,
            @RequestBody ProductSummaryRequest request
    ) {
        // TODO: Implement service call
        // ProductDailyResponse response = productSummaryService.getProductDaily(productId, request);

        // Mock
        ProductDailyResponse response = new ProductDailyResponse();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products-chart")
    public ResponseEntity<ProductsChartResponse> getProductsChart(@RequestBody ProductChartRequest request) {
        // TODO: Implement service call
        // ProductsChartResponse response = productChartService.getProductsChart(request);

        // Mock
        ProductsChartResponse response = new ProductsChartResponse();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/product-chart/{productId}")
    public ResponseEntity<ProductChartResponse> getProductChart(
            @PathVariable(name = "productId") String productId,
            @RequestBody ProductChartRequest request
    ) {
        // TODO: Implement service call
        // ProductChartResponse response = productChartService.getProductChart(productId, request);

        // Mock
        ProductChartResponse response = new ProductChartResponse();
        return ResponseEntity.ok(response);
    }
}
