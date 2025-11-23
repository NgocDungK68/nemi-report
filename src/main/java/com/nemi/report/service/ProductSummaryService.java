package com.nemi.report.service;

import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;

public interface ProductSummaryService {

    ProductSummaryResponse getProductSummary(ReportSummaryRequest request);
    ProductDailyResponse getProductDaily(ReportSummaryRequest request, String productId);
}
