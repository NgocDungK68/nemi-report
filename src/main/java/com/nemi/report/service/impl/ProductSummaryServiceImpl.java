package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ProductConfig;
import com.nemi.report.constant.ColumnDataType;
import com.nemi.report.constant.Limit;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.constant.ReportConstants;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.OrderParameter;
import com.nemi.report.model.QueryParameter;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.ProductReportModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.PageCountData;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.repository.ProductCustomRepository;
import com.nemi.report.service.ProductSummaryService;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.nemi.report.util.DateUtils.convertInstantToString;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSummaryServiceImpl implements ProductSummaryService {
    private final ProductCustomRepository productCustomRepository;
    private final ProductConfig productConfig;
    private final ClaimUtil claimUtil;

    private static final List<String> excludeColumns = List.of("product_id", "product_name", "status", "created_time");

    @Override
    public ProductSummaryResponse getProductSummary(ReportSummaryRequest request) {
        try {
            PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

            // fetch ProductReportModel
            List<ProductReportModel> productReportModels = fetchProductReportModels(
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getColumns(),
                    request.getFilters(),
                    pageRequest,
                    null,
                    null
            );

            // query pagination data
            PageCountData pageCountData = productCustomRepository.count(
                    convertToQueryParameters(request.getFilters()),
                    request.getStartDate(),
                    request.getEndDate(),
                    pageRequest,
                    claimUtil.getDepartmentId(),
                    null
            );

            // query summary
            Map<String, Object> summary = getSummary(
                    request.getColumns(),
                    request.getFilters(),
                    request.getStartDate(),
                    request.getEndDate(),
                    null
            );

            return buildProductSummaryResponse(productReportModels, pageCountData, summary);
        } catch (Exception e) {
            log.error("[getProductSummary] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductDailyResponse getProductDaily(ReportSummaryRequest request, String productId) {
        try {
            PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());

            // fetch ProductReportModel
            List<ProductReportModel> productReportModels = fetchProductReportModels(
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getColumns(),
                    request.getFilters(),
                    pageRequest,
                    null,
                    productId
            );

            // query pagination data
            PageCountData pageCountData = productCustomRepository.count(
                    convertToQueryParameters(request.getFilters()),
                    request.getStartDate(),
                    request.getEndDate(),
                    pageRequest,
                    claimUtil.getDepartmentId(),
                    productId
            );

            // query summary
            Map<String, Object> summary = getSummary(
                    request.getColumns(),
                    request.getFilters(),
                    request.getStartDate(),
                    request.getEndDate(),
                    productId
            );

            return buildProductDailyResponse(productReportModels, pageCountData, summary);
        } catch (Exception e) {
            log.error("[getProductDaily] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private LinkedHashSet<QueryParameter> convertToQueryParameters(List<FilterRequest> filters) {
        LinkedHashSet<QueryParameter> queryParameters = new LinkedHashSet<>();

        if (ObjectUtils.isNotEmpty(filters)) {
            filters.forEach(filter -> {
                ColumnConfig columnConfig = productConfig.getColumnByCode(filter.getCode());
                if (columnConfig != null) {
                    queryParameters.add(new QueryParameter(columnConfig, filter.getType(), new ArrayList<>(filter.getValue())));
                }
            });
        }

        return queryParameters;
    }

    public List<ProductReportModel> fetchProductReportModels(CurrencyCodeEnum currency,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             List<ColumnRequest> columns,
                                                             List<FilterRequest> filters,
                                                             PageRequest pageRequest,
                                                             Limit limit,
                                                             String productId) {
        LinkedHashSet<ColumnConfig> viewColumns = new LinkedHashSet<>();
        LinkedHashSet<ColumnConfig> searchColumns = new LinkedHashSet<>();
        LinkedHashSet<OrderParameter> orderParameters = new LinkedHashSet<>();
        LinkedHashSet<QueryParameter> queryParameters = convertToQueryParameters(filters);

        columns.forEach(column -> {
            ColumnConfig columnConfig = productConfig.getColumnByCode(column.getCode());
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

        // query data
        List<Map<String, Object>> data = productCustomRepository.search(
                new LinkedHashSet<>(searchColumns),
                queryParameters, orderParameters, limit,
                startDate, endDate, pageRequest,
                claimUtil.getDepartmentId(), productId
        );

        List<ProductReportModel> productReportModels = new ArrayList<>();

        for (Map<String, Object> row : data) {
            ProductReportModel productReportModel = new ProductReportModel();
            Map<String, Object> extraData = new HashMap<>();

            row.forEach((key, value) -> {
                if (StringUtils.equals(key, ReportConstants.PRODUCT_ID)) {
                    productReportModel.setProductId(String.valueOf(value));
                } else if (StringUtils.equals(key, ReportConstants.PRODUCT_NAME)) {
                    productReportModel.setProductName(String.valueOf(value));
                } else if (StringUtils.equals(key, ReportConstants.PRODUCT_IMAGE)) {
                    productReportModel.setProductImage(String.valueOf(value));
                } else if (StringUtils.equals(key, ReportConstants.REPORT_DATE)) {
                    productReportModel.setReportDate(String.valueOf(value));
                } else if (viewColumns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), key))) {
                    ColumnConfig matchColumn = viewColumns.stream().filter(c -> StringUtils.equals(c.getCode(), key)).findFirst().orElse(null);

                    if (ObjectUtils.isNotEmpty(matchColumn) && ObjectUtils.isNotEmpty(value)) {
                        if (value instanceof Number n) {
                            extraData.put(key, n);
                        } else if (matchColumn.getType().equals(ColumnDataType.TIMESTAMP)) {
                            if (value instanceof Timestamp ts) {
                                extraData.put(key, convertInstantToString(ts.toInstant()));

                            } else if (value instanceof Instant i) {
                                extraData.put(key, convertInstantToString(i));

                            } else {
                                extraData.put(key, null);
                            }
                        } else {
                            extraData.put(key, value);
                        }
                    }
                }
            });

            productReportModel.setExtraData(extraData);
            productReportModels.add(productReportModel);
        }

        return productReportModels;
    }

    private ProductSummaryResponse buildProductSummaryResponse(List<ProductReportModel> productReportModels,
                                                               PageCountData pageCountData,
                                                               Map<String, Object> summary) {
        List<ProductSummaryResponse.DataItem> dataItems = new ArrayList<>();

        for (ProductReportModel productReportModel : productReportModels) {
            ProductSummaryResponse.DataItem dataItem = new ProductSummaryResponse.DataItem();
            Map<String, Object> extraData = productReportModel.getExtraData();

            dataItem.setProduct(ProductSummaryResponse.ProductData.builder()
                    .id(productReportModel.getProductId())
                    .name(productReportModel.getProductName())
                    .imageUrl(productReportModel.getProductImage())
                    .build());
            dataItem.setExtraData(extraData);
            dataItems.add(dataItem);
        }

        return ProductSummaryResponse.builder()
                .totalElements(pageCountData.getTotalElements())
                .totalPages(pageCountData.getTotalPages())
                .data(dataItems)
                .summary(summary)
                .build();
    }

    private ProductDailyResponse buildProductDailyResponse(List<ProductReportModel> productReportModels,
                                                           PageCountData pageCountData,
                                                           Map<String, Object> summary) {
        List<ProductDailyResponse.DataItem> dataItems = new ArrayList<>();

        for (ProductReportModel productReportModel : productReportModels) {
            ProductDailyResponse.DataItem dataItem = new ProductDailyResponse.DataItem();
            Map<String, Object> extraData = productReportModel.getExtraData();

            dataItem.setDate(productReportModel.getReportDate());
            dataItem.setExtraData(extraData);
            dataItems.add(dataItem);
        }

        return ProductDailyResponse.builder()
                .totalElements(pageCountData.getTotalElements())
                .totalPages(pageCountData.getTotalPages())
                .data(dataItems)
                .summary(summary)
                .build();
    }

    public Map<String, Object> getSummary(List<ColumnRequest> columns,
                                          List<FilterRequest> filters,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          String productId) {
        LinkedHashSet<QueryParameter> queryParameters = convertToQueryParameters(filters);
        LinkedHashSet<ColumnConfig> columnConfigs = new LinkedHashSet<>();

        columns.forEach(column -> {
            ColumnConfig columnConfig = productConfig.getColumnByCode(column.getCode());
            if (columnConfig != null) {
                if (!excludeColumns.contains(columnConfig.getCode())) {
                    columnConfigs.add(columnConfig);
                }

                if (columnConfig.getRequiredForAvg() != null) {
                    columnConfig.getRequiredForAvg().forEach(subColumnCode -> {
                        ColumnConfig subColumnConfig = productConfig.getColumnByCode(subColumnCode);
                        columnConfigs.add(subColumnConfig);
                    });
                }
            }
        });

        return productCustomRepository.summary(columnConfigs, queryParameters, startDate, endDate, claimUtil.getDepartmentId(), productId);
    }
}
