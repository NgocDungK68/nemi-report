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
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
                    request.getLimit(),
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

            return buildProductsChartResponse(request, productReportModels, summary, request.isSplitByDate(), request.getStartDate(), request.getEndDate(), code);
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
                    request.getLimit(),
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

    private ProductsChartResponse buildProductsChartResponse(ReportChartRequest request,
                                                             List<ProductReportModel> productReportModels,
                                                             Map<String, Object> summary,
                                                             boolean splitByDate,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             String code) {
        BigDecimal summaryValue = sumData(productReportModels, code);
        int scale = reportConfig.getScale().getPercent();

        // build summary each day from startDate to endDate
        Map<LocalDate, BigDecimal> summaryEachDay = new HashMap<>();
        Map<String, ProductChartResponse> productMap = new HashMap<>();

        if (splitByDate) {
            summaryEachDay = buildSummaryEachDay(productReportModels, request, startDate, endDate);

            // build product map
            for (ProductReportModel productReportModel : productReportModels) {
                String productId = productReportModel.getProductId();
                ProductChartResponse productChartResponse = getProductChartByProductId(productId, request);
                productMap.put(productId, productChartResponse);
            }
        }

        List<ProductsChartResponse.ProductData> productDataList = new ArrayList<>();
        for (ProductReportModel productReportModel : productReportModels) {
            ProductsChartResponse.ProductData productData = new ProductsChartResponse.ProductData();
            String productId = productReportModel.getProductId();

            // set product info
            productData.setProduct(ProductsChartResponse.Product.builder()
                    .id(productId)
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

            // build dateValues
            if (splitByDate) {
                List<ProductsChartResponse.DateValue> dateValues = buildDateValues(
                        productMap, summaryEachDay, productId,
                        startDate, endDate, scale
                );

                productData.setDateValues(dateValues);
            }

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
                    .date(productReportModel.getReportDate())
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

    private Map<LocalDate, BigDecimal> buildSummaryEachDay(List<ProductReportModel> productReportModels,
                                                           ReportChartRequest request,
                                                           LocalDate startDate,
                                                           LocalDate endDate) {
        Map<LocalDate, BigDecimal> summaryEachDay = new HashMap<>();
        Map<String, ProductChartResponse> productMap = new HashMap<>();
        for (ProductReportModel productReportModel : productReportModels) {
            String productId = productReportModel.getProductId();
            ProductChartResponse productChartResponse = getProductChartByProductId(productId, request);
            productMap.put(productId, productChartResponse);
        }

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            BigDecimal sum = BigDecimal.ZERO;
            for (ProductReportModel productReportModel : productReportModels) {
                ProductChartResponse.DateData data = getDateData(productMap, productReportModel.getProductId(),currentDate);
                if (ObjectUtils.isNotEmpty(data)) sum = sum.add(data.getValue());
            }

            summaryEachDay.put(currentDate, sum);
            currentDate = currentDate.plusDays(1);
        }

        return summaryEachDay;
    }

    private ProductChartResponse.DateData getDateData(Map<String, ProductChartResponse> productMap,
                                                      String productId,
                                                      LocalDate date) {
        ProductChartResponse productChartResponse = productMap.get(productId);
        String dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return productChartResponse.getDateData().stream()
                .filter(dateData -> dateString.equals(dateData.getDate()))
                .findFirst()
                .orElse(null);
    }

    private List<ProductsChartResponse.DateValue> buildDateValues(Map<String, ProductChartResponse> productMap,
                                                                  Map<LocalDate, BigDecimal> summaryEachDay,
                                                                  String productId,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  int scale) {
        List<ProductsChartResponse.DateValue> dateValues = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            ProductChartResponse.DateData dataCurrentDate = getDateData(productMap, productId, currentDate);
            BigDecimal valueCurrentDate = BigDecimal.ZERO;
            if (dataCurrentDate != null) {
                valueCurrentDate = ReportUtils.convertToBigDecimal(dataCurrentDate.getValue());
            }

            BigDecimal percentCurrentDate = ReportUtils.calculatePercentage(valueCurrentDate, summaryEachDay.get(currentDate), scale);

            ProductsChartResponse.DateValue dateValue = ProductsChartResponse.DateValue.builder()
                    .date(currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .value(valueCurrentDate)
                    .percent(percentCurrentDate)
                    .build();

            dateValues.add(dateValue);
            currentDate = currentDate.plusDays(1);
        }

        return dateValues;
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
