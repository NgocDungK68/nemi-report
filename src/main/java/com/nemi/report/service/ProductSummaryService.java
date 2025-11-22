package com.nemi.report.service;

import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;

public interface ProductSummaryService {

    ProductSummaryResponse getProductSummary(ProductSummaryRequest request);
    ProductDailyResponse getProductDaily(ProductSummaryRequest request, String productId);
}
