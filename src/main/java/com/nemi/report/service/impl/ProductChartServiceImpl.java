package com.nemi.report.service.impl;

import com.nemi.report.model.request.product.ProductChartRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.service.ProductChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductChartServiceImpl implements ProductChartService {
    @Override
    public ProductsChartResponse getProductsChart(ProductChartRequest productChartRequest) {
        return null;
    }

    @Override
    public ProductChartResponse getProductChartByProductId(String productId, ProductChartRequest productChartRequest) {
        return null;
    }
}
