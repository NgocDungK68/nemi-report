package com.nemi.report.service.impl;

import com.nemi.report.configuration.ProductConfig;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.repository.ProductCustomRepository;
import com.nemi.report.service.ProductChartService;
import com.nemi.report.util.DateUtils;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductChartServiceImpl implements ProductChartService {
    private final ProductConfig productConfig;
    private final ProductCustomRepository productCustomRepository;
    private final ClaimUtil claimUtil;
    private final ProductSummaryServiceImpl productSummaryService;

    @Override
    public ProductsChartResponse getProductsChart(ReportChartRequest request) {
        String code = request.getChartData().getCode();
        ProductSummaryResponse summaryResponse = getSummaryResponse(
                code,
                request.getStartDate(),
                request.getEndDate(),
                null
        );

        // summary data
        ProductsChartResponse.Summary summary = getSummary(summaryResponse, code);

        List<ProductsChartResponse.ProductData> productDataList = new ArrayList<>();
        int totalElements = 0;
        for (ProductSummaryResponse.DataItem dataItem : summaryResponse.getData()) {
            productDataList.add(getProductData(dataItem, summary, code));
            totalElements += 1;
        }

        return ProductsChartResponse.builder()
                .totalElements(totalElements)
                .productData(productDataList)
                .summary(summary)
                .build();
    }

    @Override
    public ProductChartResponse getProductChartByProductId(String productId, ReportChartRequest request) {
        String code = request.getChartData().getCode();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // summary data
        ProductSummaryResponse summaryResponse = getSummaryResponse(code, request.getStartDate(), request.getEndDate(), productId);
        ProductsChartResponse.Summary summary = getSummary(summaryResponse, code);

        List<ProductChartResponse.DateData> dateDataList = new ArrayList<>();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            ProductSummaryResponse summaryResponseByDay = getSummaryResponse(code, date, date, productId);
            List<ProductSummaryResponse.DataItem> items = summaryResponseByDay.getData();

            BigDecimal value = BigDecimal.ZERO;
            if (ObjectUtils.isNotEmpty(items)) {
                value = toBigDecimal(items.get(0).getExtraData().get(code));
            }

            ProductChartResponse.DateData dateData = ProductChartResponse.DateData.builder()
                    .value(value)
                    .percent(summary.getValue().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : value.divide(summary.getValue(), RoundingMode.HALF_UP))
                    .build();

            dateDataList.add(dateData);
        }

        return ProductChartResponse.builder()
                .totalElements(DateUtils.daysBetweenInclusive(startDate, endDate))
                .dateData(dateDataList)
                .summary(summary)
                .build();
    }

    private ProductSummaryResponse getSummaryResponse(String code, LocalDate startDate, LocalDate endDate, String productId) {
        LinkedHashSet<ColumnConfig> viewColumns = new LinkedHashSet<>();
        LinkedHashSet<ColumnConfig> searchColumns = new LinkedHashSet<>();

        ColumnConfig columnConfig = productConfig.getColumnByCode(code);
        if (ObjectUtils.isNotEmpty(columnConfig)) {
            viewColumns.add(columnConfig);
            if (Objects.nonNull(columnConfig.getSubColumns())) {
                columnConfig.getSubColumns().forEach(subColumn -> {
                    ColumnConfig subColumnConfig = productConfig.getColumnByCode(subColumn);
                    if (ObjectUtils.isNotEmpty(subColumnConfig)) {
                        searchColumns.add(subColumnConfig);
                    }
                });
            }
            searchColumns.add(columnConfig);
        }

        List<Map<String, Object>> data = productCustomRepository.search(new LinkedHashSet<>(searchColumns), null, null, startDate, endDate, null, claimUtil.getUserName(), productId);

        return productSummaryService.getFromResultSQL(data, viewColumns, startDate, endDate, null, null);
    }

    private ProductsChartResponse.ProductData getProductData(ProductSummaryResponse.DataItem dataItem,
                                                             ProductsChartResponse.Summary summary,
                                                             String code) {
        // product info
        ProductsChartResponse.Product product = ProductsChartResponse.Product.builder()
                .id(dataItem.getProduct().getId())
                .name(dataItem.getProduct().getName())
                .build();

        // data
        BigDecimal value = toBigDecimal(dataItem.getExtraData().get(code));
        ProductsChartResponse.DataValue dataValue = ProductsChartResponse.DataValue.builder()
                .value(value)
                .percent(value.divide(summary.getValue(), RoundingMode.HALF_UP))
                .build();

        return ProductsChartResponse.ProductData.builder()
                .product(product)
                .data(dataValue)
                .build();
    }

    private ProductsChartResponse.Summary getSummary(ProductSummaryResponse summaryResponse, String code) {
        BigDecimal summaryValue = BigDecimal.ZERO;
        for (ProductSummaryResponse.DataItem dataItem : summaryResponse.getData()) {
            summaryValue = summaryValue.add(toBigDecimal(dataItem.getExtraData().get(code)));

            // TODO: summary.percent
        }

        return ProductsChartResponse.Summary.builder()
                .value(summaryValue)
                .build();
    }

    /**
     * Chuyển Object sang BigDecimal an toàn.
     * Hỗ trợ Long, Integer, Double, Float, BigDecimal, String.
     * Trả về null nếu object là null hoặc không chuyển được.
     */
    private BigDecimal toBigDecimal(Object valueObj) {
        if (valueObj == null) return null;

        if (valueObj instanceof BigDecimal) {
            return (BigDecimal) valueObj;
        } else if (valueObj instanceof Long) {
            return BigDecimal.valueOf((Long) valueObj);
        } else if (valueObj instanceof Integer) {
            return BigDecimal.valueOf((Integer) valueObj);
        } else if (valueObj instanceof Double) {
            return BigDecimal.valueOf((Double) valueObj);
        } else if (valueObj instanceof Float) {
            return BigDecimal.valueOf(((Float) valueObj).doubleValue());
        } else if (valueObj instanceof String) {
            try {
                return new BigDecimal((String) valueObj);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            try {
                return new BigDecimal(valueObj.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

}
