package com.nemi.report.service.impl;

import com.nemi.report.configuration.ProductConfig;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.request.ReportChartRequest;
import com.nemi.report.model.response.PageCountData;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.repository.ProductCustomRepository;
import com.nemi.report.service.ProductChartService;
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
    private final ProductSumaryServiceImpl productSummaryService;

    @Override
    public ProductsChartResponse getProductsChart(ReportChartRequest request) {
        LinkedHashSet<ColumnConfig> viewColumns = new LinkedHashSet<>();
        LinkedHashSet<ColumnConfig> searchColumns = new LinkedHashSet<>();

        String code = request.getChartData().getCode();
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

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        List<Map<String, Object>> data = productCustomRepository.search(new LinkedHashSet<>(searchColumns), null, null, start, end, null, claimUtil.getUserName(), null);

        ProductSummaryResponse summaryResponse = productSummaryService.getFromResultSQL(data, viewColumns, start, end, 0, 0);

        ProductsChartResponse.Summary summary = getSummary(summaryResponse, code);

        List<ProductsChartResponse.UserData> userDataList = new ArrayList<>();
        int totalElements = 0;
        for (ProductSummaryResponse.DataItem dataItem : summaryResponse.getData()) {
            userDataList.add(getUserData(dataItem, summary, code));
            totalElements += 1;
        }

        return ProductsChartResponse.builder()
                .totalElements(totalElements)
                .userData(userDataList)
                .summary(summary)
                .build();
    }

    @Override
    public ProductChartResponse getProductChartByProductId(String productId, ReportChartRequest reportChartRequest) {
        return null;
    }

    private ProductsChartResponse.UserData getUserData(ProductSummaryResponse.DataItem dataItem,
                                                       ProductsChartResponse.Summary summary,
                                                       String code) {
        ProductsChartResponse.ProductData productData = ProductsChartResponse.ProductData.builder()
                .id(dataItem.getProduct().getId())
                .name(dataItem.getProduct().getName())
                .build();

        BigDecimal value = toBigDecimal(dataItem.getExtraData().get(code));
        ProductsChartResponse.DataValue dataValue = ProductsChartResponse.DataValue.builder()
                .value(value)
                .percent(value.divide(summary.getValue(), RoundingMode.HALF_UP))
                .build();

        return ProductsChartResponse.UserData.builder()
                .product(productData)
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
            // các kiểu khác, thử dùng toString()
            try {
                return new BigDecimal(valueObj.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

}
