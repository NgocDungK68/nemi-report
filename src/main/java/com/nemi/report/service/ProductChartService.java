package com.nemi.report.service;

import com.nemi.report.model.request.product.ProductChartRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;

public interface ProductChartService {
    ProductsChartResponse getProductsChart(ProductChartRequest productChartRequest);
    ProductChartResponse getProductChartByProductId(String productId, ProductChartRequest productChartRequest);
}
