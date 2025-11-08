package com.nemi.report.service;

import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;

public interface ProductChartService {
    ProductsChartResponse getProductsChart(ReportChartRequest reportChartRequest);
    ProductChartResponse getProductChartByProductId(String productId, ReportChartRequest reportChartRequest);
}
