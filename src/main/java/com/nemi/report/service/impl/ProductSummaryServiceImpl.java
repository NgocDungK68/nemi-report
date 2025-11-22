package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ProductConfig;
import com.nemi.report.constant.ColumnDataType;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.constant.ReportConstants;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.OrderParameter;
import com.nemi.report.model.QueryParameter;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.ProductReportModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.model.request.product.ProductSummaryRequest;
import com.nemi.report.model.response.PageCountData;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.repository.ProductCustomRepository;
import com.nemi.report.service.ProductSummaryService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSummaryServiceImpl implements ProductSummaryService {
    private final ProductCustomRepository productCustomRepository;
    private final ProductConfig productConfig;
    private final ClaimUtil claimUtil;

    private static final List<String> excludeColumns = List.of("product_id", "product_name", "status", "created_time");

    @Override
    public ProductSummaryResponse getProductSummary(ProductSummaryRequest request) {
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

            ProductSummaryResponse response = buildProductSummaryResponse(productReportModels);
            response.setTotalElements(pageCountData.getTotalElements());
            response.setTotalPages(pageCountData.getTotalPages());

            return response;
        } catch (Exception e) {
            log.error("[getProductSummary] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductDailyResponse getProductDaily(ProductSummaryRequest request, String productId) {
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

            ProductDailyResponse response = buildProductDailyResponse(productReportModels);
            response.setTotalElements(pageCountData.getTotalElements());
            response.setTotalPages(pageCountData.getTotalPages());

            return response;
        } catch (Exception e) {
            log.error("[getProductDaily] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    public ProductSummaryResponse getFromResultSQL(List<Map<String, Object>> rows, Set<ColumnConfig> columns, LocalDate start, LocalDate end, Long totalElements, Integer totalPages) {
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
                    ColumnConfig matchColumn = columns.stream().filter(c -> StringUtils.equals(c.getCode(), key)).findFirst().orElse(null);

                    if (matchColumn != null) {
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

            if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), ReportConstants.REPORT_DATE_START))) {
                extraData.put(ReportConstants.REPORT_DATE_START, DateUtils.dateToString(start));
            }
            if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), ReportConstants.REPORT_DATE_END))) {
                extraData.put(ReportConstants.REPORT_DATE_END, DateUtils.dateToString(end));
            }


            dataItem.setProduct(productData);
            dataItem.setExtraData(extraData);

            dataList.add(dataItem);
        }


        response.setData(dataList);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);


        Map<String, Object> summary = new HashMap<>();
        summary.put(ReportConstants.TOTAL_PRODUCT, totalElements);
        response.setSummary(summary);

        return response;
    }


    private String convertInstantToString(Instant instant) {
        if (instant != null && instant.getEpochSecond() >= 0) {
            return DateUtils.instantToTimeString(instant);
        }
        return null;
    }


    private LinkedHashSet<QueryParameter> convertToQueryParameters(ProductSummaryRequest request) {
        LinkedHashSet<QueryParameter> queryParameters = new LinkedHashSet<>();

        request.getFilters().forEach(filter -> {
            ColumnConfig columnConfig = productConfig.getColumnByCode(filter.getCode());
            if (columnConfig != null) {
                queryParameters.add(new QueryParameter(columnConfig, filter.getType(), new ArrayList<>(filter.getValue())));
            }
        });
        return queryParameters;
    }

    private LinkedHashSet<QueryParameter> convertToQueryParameters(List<FilterRequest> filters) {
        LinkedHashSet<QueryParameter> queryParameters = new LinkedHashSet<>();

        filters.forEach(filter -> {
            ColumnConfig columnConfig = productConfig.getColumnByCode(filter.getCode());
            if (columnConfig != null) {
                queryParameters.add(new QueryParameter(columnConfig, filter.getType(), new ArrayList<>(filter.getValue())));
            }
        });
        return queryParameters;
    }

    private List<ProductReportModel> fetchProductReportModels(CurrencyCodeEnum currency,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             List<ColumnRequest> columns,
                                                             List<FilterRequest> filters,
                                                             PageRequest pageRequest,
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
                queryParameters, orderParameters,
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

                    if (matchColumn != null) {
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

            if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), ReportConstants.REPORT_DATE_START))) {
                extraData.put(ReportConstants.REPORT_DATE_START, DateUtils.dateToString(startDate));
            }
            if (columns.stream().anyMatch(c -> StringUtils.equals(c.getCode(), ReportConstants.REPORT_DATE_END))) {
                extraData.put(ReportConstants.REPORT_DATE_END, DateUtils.dateToString(endDate));
            }

            productReportModel.setExtraData(extraData);
            productReportModels.add(productReportModel);
        }

        return productReportModels;
    }

    private ProductSummaryResponse buildProductSummaryResponse(List<ProductReportModel> productReportModels) {
        ProductSummaryResponse response = new ProductSummaryResponse();
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

        response.setData(dataItems);
        return response;
    }

    private ProductDailyResponse buildProductDailyResponse(List<ProductReportModel> productReportModels) {
        ProductDailyResponse response = new ProductDailyResponse();
        List<ProductDailyResponse.DataItem> dataItems = new ArrayList<>();

        for (ProductReportModel productReportModel : productReportModels) {
            ProductDailyResponse.DataItem dataItem = new ProductDailyResponse.DataItem();
            Map<String, Object> extraData = productReportModel.getExtraData();

            dataItem.setDate(productReportModel.getReportDate());
            dataItem.setExtraData(extraData);
            dataItems.add(dataItem);
        }

        response.setData(dataItems);
        return response;
    }
}
