package com.nemi.report.service;

import com.nemi.model.response.PageResponse;
import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.product.ProductSummaryResponse;

import java.util.List;

public interface ProductSumaryService {

    ProductSummaryResponse getProductSumary(ProductSummaryRequest request);
}
