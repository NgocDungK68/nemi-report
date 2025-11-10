package com.nemi.report.service.impl;

import com.nemi.report.configuration.ProductConfig;
import com.nemi.report.constant.ColumnDataType;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.model.OrderParameter;
import com.nemi.report.model.QueryParameter;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.PageCountData;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.repository.ProductCustomRepository;
import com.nemi.report.service.ProductSumaryService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.DateUtils;
import com.nemi.util.LanguageChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSumaryServiceImpl implements ProductSumaryService {
    private final ProductCustomRepository productCustomRepository;
    private final ProductConfig productConfig;
    private final ClaimUtil claimUtil;
    private final HttpServletRequest httpServletRequest;

    private static final List<String> excludeColumns = List.of("product_id","product_name", "status","created_time");

    @Override
    public ProductSummaryResponse getProductSumary(ProductSummaryRequest request,String productid) {
        LinkedHashSet<ColumnConfig> viewColumns = new LinkedHashSet<>();
        LinkedHashSet<ColumnConfig> searchColumns = new LinkedHashSet<>();

        LinkedHashSet<OrderParameter> orderParameters = new LinkedHashSet<>();
        request.getColumns().forEach(column -> {
            ColumnConfig columnConfig = productConfig.getColumnByCode(column.getCode());
// da dc
            if (columnConfig != null) {
                // Exclude columns that already in the search
                if (!excludeColumns.contains(columnConfig.getCode())) {
                    viewColumns.add(columnConfig);
                    // if column has sub columns, add them to search columns
                    if (Objects.nonNull(columnConfig.getSubColumns())) {
                        columnConfig.getSubColumns().forEach(subColumn -> {
                            ColumnConfig subColumnConfig = productConfig.getColumnByCode(subColumn);
                            if (subColumnConfig != null) {
                                searchColumns.add(subColumnConfig);
                            }
                        });
                    } else if (!columnConfig.getSource().equals(ProductSource.VIEW_ONLY)) {
                        searchColumns.add(columnConfig);
                    }
                }

                if (Objects.nonNull(column.getOrder())) {
                    orderParameters.add(new OrderParameter(columnConfig, column.getOrder()));
                }

            }
        });

        LinkedHashSet<QueryParameter> queryParameters = convertToQueryParameters(request);

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        List<Map<String, Object>> data = productCustomRepository.search(new LinkedHashSet<>(searchColumns), queryParameters, orderParameters, start, end, pageRequest, claimUtil.getUserName(), productid);

        PageCountData countData = productCustomRepository.count(queryParameters, start, end, pageRequest, claimUtil.getUserName(),productid);

        ProductSummaryResponse response = getFromResultSQL(data, viewColumns, start, end, countData.getTotalElements(), countData.getTotalPages());
        return response;
    }

    public ProductSummaryResponse getFromResultSQL(
            List<Map<String, Object>> rows,
            Set<ColumnConfig> columns,
            LocalDate start,
            LocalDate end,
            long totalElements,
            int totalPages
    ) {
        ProductSummaryResponse response = new ProductSummaryResponse();
        List<ProductSummaryResponse.DataItem> dataList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            ProductSummaryResponse.DataItem dataItem = new ProductSummaryResponse.DataItem();
            ProductSummaryResponse.ProductData productData = new ProductSummaryResponse.ProductData();
            Map<String, Object> extraData = new HashMap<>();

            row.forEach((key, value) -> {

                if (StringUtils.equals(key, "product_id")) {
                    productData.setId(String.valueOf(value));
                } else if (StringUtils.equals(key, "name")) {
                    productData.setName(String.valueOf(value));
                } else if (StringUtils.equals(key, "images")) {
                    productData.setImageUrl(String.valueOf(value));


                } else if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), key))) {
                    ColumnConfig matchColumn = columns.stream()
                            .filter(c -> StringUtils.equals(c.getCode(), key))
                            .findFirst()
                            .orElse(null);

                    if (matchColumn != null) {
                        if (value instanceof Number n) {
                            extraData.put(key, n);
                        } else if (matchColumn.getType().equals(ColumnDataType.TIMESTAMP)) {
                            extraData.put(key, convertInstantToString((Instant) value, matchColumn));
                        } else {
                            extraData.put(key, value);
                        }
                    }
                }
            });

            if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), "report_date_start"))) {
                extraData.put("report_date_start", DateUtils.dateToString(start));
            }
            if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), "report_date_end"))) {
                extraData.put("report_date_end", DateUtils.dateToString(end));
            }


            dataItem.setProduct(productData);
            dataItem.setExtraData(extraData);

            dataList.add(dataItem);
        }


        response.setData(dataList);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);


        Map<String, Object> summary = new HashMap<>();
        summary.put("total_products", totalElements);
        summary.put("report_range", start + " → " + end);
        response.setSummary(summary);

        return response;
    }


    private String convertInstantToString(Instant instant, ColumnConfig column) {
        boolean isEn = LanguageChecker.isEn(httpServletRequest);
        if (instant != null && instant.getEpochSecond() >= 0) {
            return DateUtils.instantToTimeString(instant);
        } else if (StringUtils.equals(column.getCode(), "end_time") || StringUtils.equals(column.getCode(), "ad_end_time")) {
            return isEn ? "Is going on" : "Đang diễn ra";
        }
        return null;
    }


    private LinkedHashSet<QueryParameter> convertToQueryParameters(ProductSummaryRequest request) {
        LinkedHashSet<QueryParameter> queryParameters = new LinkedHashSet<>();

        request.getFilters()
                .forEach(filter -> {
                    ColumnConfig columnConfig = productConfig.getColumnByCode(filter.getCode());
                    if (columnConfig != null) {
                        queryParameters.add(new QueryParameter(columnConfig, filter.getType(), new ArrayList<>(filter.getValue())));
                    }
                });
        return queryParameters;
    }

}
