package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.pojo.ProductReportModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.service.ProductChartService;
import com.nemi.report.util.ReportUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductChartServiceImpl implements ProductChartService {
    private final ProductSummaryServiceImpl productSummaryService;
    private final ReportConfig reportConfig;

    @Override
    public ProductsChartResponse getProductsChart(ReportChartRequest request) {
        try {
            String code = request.getChartData().getCode();
            List<ColumnRequest> columnRequest = buildColumnRequest(code);

            // fetch ProductReportModel
            List<ProductReportModel> productReportModels = productSummaryService.fetchProductReportModels(
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    columnRequest,
                    null,
                    null,
                    null
            );

            // query summary
            Map<String, Object> summary = productSummaryService.getSummary(
                    columnRequest,
                    null,
                    request.getStartDate(),
                    request.getEndDate(),
                    null
            );

            return buildProductsChartResponse(productReportModels, summary, code);
        } catch (Exception e) {
            log.error("[getProductsChart] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductChartResponse getProductChartByProductId(String productId, ReportChartRequest request) {
        try {
            String code = request.getChartData().getCode();
            List<ColumnRequest> columnRequest = buildColumnRequest(code);

            // fetch ProductReportModel
            List<ProductReportModel> productReportModels = productSummaryService.fetchProductReportModels(
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    columnRequest,
                    null,
                    null,
                    productId
            );

            // query summary
            Map<String, Object> summary = productSummaryService.getSummary(
                    columnRequest,
                    null,
                    request.getStartDate(),
                    request.getEndDate(),
                    productId
            );

            return buildProductChartResponse(productReportModels, summary, code);
        } catch (Exception e) {
            log.error("[getProductChartByProductId] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private List<ColumnRequest> buildColumnRequest(String code) {
        return List.of(new ColumnRequest(code, null));
    }

    private ProductsChartResponse buildProductsChartResponse(List<ProductReportModel> productReportModels,
                                                             Map<String, Object> summary,
                                                             String code) {
        BigDecimal summaryValue = sumData(productReportModels, code);
        int scale = reportConfig.getScale().getPercent();

        List<ProductsChartResponse.ProductData> productDataList = new ArrayList<>();
        for (ProductReportModel productReportModel : productReportModels) {
            ProductsChartResponse.ProductData productData = new ProductsChartResponse.ProductData();

            // set product info
            productData.setProduct(ProductsChartResponse.Product.builder()
                    .id(productReportModel.getProductId())
                    .name(productReportModel.getProductName())
                    .build());

            // set dataValue
            Object data = productReportModel.getExtraData().getOrDefault(code, null);
            BigDecimal value = ReportUtils.convertToBigDecimal(data);
            BigDecimal percent = ReportUtils.calculatePercentage(value, summaryValue, scale);

            productData.setData(ProductsChartResponse.DataValue.builder()
                    .value(value)
                    .percent(percent)
                    .build());

            // TODO: dateValues

            // add to productDataList
            productDataList.add(productData);
        }

        // build summary
        ProductsChartResponse.Summary summaryData = buildSummaryData(summary, summaryValue, scale, code);

        return ProductsChartResponse.builder()
                .totalElements(productDataList.size())
                .productData(productDataList)
                .summary(summaryData)
                .build();
    }

    private ProductChartResponse buildProductChartResponse(List<ProductReportModel> productReportModels,
                                                           Map<String, Object> summary,
                                                           String code) {
        BigDecimal summaryValue = sumData(productReportModels, code);
        int scale = reportConfig.getScale().getPercent();

        List<ProductChartResponse.DateData> dateDataList = new ArrayList<>();
        for (ProductReportModel productReportModel : productReportModels) {
            // set dateData
            Object data = productReportModel.getExtraData().getOrDefault(code, null);
            BigDecimal value = ReportUtils.convertToBigDecimal(data);
            BigDecimal percent = ReportUtils.calculatePercentage(value, summaryValue, scale);

            ProductChartResponse.DateData dateData = ProductChartResponse.DateData.builder()
                    .value(value)
                    .percent(percent)
                    .build();

            // add to dateDataList
            dateDataList.add(dateData);
        }

        // build summary
        ProductsChartResponse.Summary summaryData = buildSummaryData(summary, summaryValue, scale, code);

        return ProductChartResponse.builder()
                .totalElements(dateDataList.size())
                .dateData(dateDataList)
                .summary(summaryData)
                .build();
    }

    private ProductsChartResponse.Summary buildSummaryData(Map<String, Object> summary, BigDecimal summaryValue, int scale, String code) {
        Object summaryAll = summary.getOrDefault(code, null);
        BigDecimal summaryAllValue = ReportUtils.convertToBigDecimal(summaryAll);
        BigDecimal summaryPercent = ReportUtils.calculatePercentage(summaryValue, summaryAllValue, scale);

        return ProductsChartResponse.Summary.builder()
                .value(summaryValue)
                .percent(summaryPercent)
                .build();
    }

    private BigDecimal sumData(List<ProductReportModel> productReportModels, String code) {
        List<Number> listData = new ArrayList<>();
        productReportModels.forEach(productReportModel -> {
            Object value = productReportModel.getExtraData().get(code);
            if (value instanceof Number number) {
                listData.add(number);
            }
        });

        return ReportUtils.sum(listData);
    }
}
