package com.nemi.report.service;

import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.product.ProductChartRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;

public interface ProductReportService {

    ProductSummaryResponse getProductSummary(ReportSummaryRequest request);

    ProductDailyResponse getProductDaily(String productId, ReportSummaryRequest request);

    ProductsChartResponse getAllProductsChart(ProductChartRequest request);

    ProductChartResponse getProductChart(String productId, ProductChartRequest request);
}
