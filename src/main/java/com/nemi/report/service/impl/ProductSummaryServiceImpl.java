package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ProductColumnConfig;
import com.nemi.report.constant.Limit;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByProductModel;
import com.nemi.report.model.pojo.ReportByDateModel;
import com.nemi.report.model.pojo.ReportByProductModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.product.ProductChartRequest;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.model.response.product.ProductChartResponse;
import com.nemi.report.model.response.product.ProductDailyResponse;
import com.nemi.report.model.response.product.ProductSummaryResponse;
import com.nemi.report.model.response.product.ProductsChartResponse;
import com.nemi.report.repository.OrderCustomRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.ExchangeRateService;
import com.nemi.report.service.ProductSummaryService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ReportValidator;
import com.nemi.util.ClaimUtil;
import com.nemi.util.CurrencyUtils;
import com.nemi.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.nemi.report.util.ReportUtils.convertToBigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSummaryServiceImpl implements ProductSummaryService {
    private final ClaimUtil claimUtil;
    private final ConfigService configService;
    private final ProductColumnConfig productColumnConfig;
    private final ExchangeRateService exchangeRateService;
    private final OrderCustomRepository orderCustomRepository;
    private final ReportHelperService reportHelperService;

    @Override
    public ProductSummaryResponse getProductSummary(ReportSummaryRequest request) {
        try {
            // 1. filter request
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), productColumnConfig.getListColumns());
            List<ColumnRequest> validOrders = ReportValidator.getValidOrders(request.getColumns(), productColumnConfig.getListColumns());
            List<FilterRequest> validFilters = ReportValidator.getValidFilters(request.getFilters(), productColumnConfig.getListColumns());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Get exchange rate
            BigDecimal lastExchangeRate = exchangeRateService.getLastExchangeRate(claimUtil.getCompanyId(), CurrencyCodeEnum.VND.name(), request.getCurrency().name(), request.getStartDate(), request.getEndDate());

            // 4. Inquiry product with order
            List<OrderQueryByProductModel> orderQueryModels = orderCustomRepository.reportOrderByProduct(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    validOrders, validFilters,
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Exchange rate
            applyExchangeRatesToOrders(orderQueryModels, lastExchangeRate, request.getCurrency());

            // 6. TODO: get ad cost if needed

            // 7. Combine order and ads cost
            List<ReportByProductModel> reportModels = combineDataReportProducts(orderQueryModels);

            // 8. Pagination
            List<ReportByProductModel> pagedModels = reportHelperService.paginateReportModels(reportModels, request.getPage(), request.getSize());

            // 9. Map to response
            ProductSummaryResponse response = buildProductSummaryResponse(pagedModels, reportModels, validColumns);
            response.setTotalElements((long) reportModels.size());
            response.setTotalPages(ReportUtils.calculateTotalPages(reportModels.size(), request.getSize()));

            return response;
        } catch (Exception e) {
            log.error("[getProductSummary] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductDailyResponse getProductDaily(ReportSummaryRequest request, String productId) {
        try {
            // 1. Get columns
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), productColumnConfig.getListColumns());
            ReportSettingResponse reportSetting = configService.getConfig();

            // 2. Get list users with orders
            List<OrderQueryByDateModel> orderQueryModel = fetchOrderQueryByProductDateModels(
                    productId,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting
            );

            // 3. TODO: get ad cost if needed

            // 4. Combine data
            List<ReportByDateModel> reportModels = combineDataReportProductDate(orderQueryModel, request.getStartDate(), request.getEndDate());

            // 5. Pagination
            List<ReportByDateModel> pagedModels = reportHelperService.paginateReportModels(reportModels, request.getPage(), request.getSize());

            // 6. Build response
            ProductDailyResponse response = buildReportProductDailyResponse(pagedModels, reportModels, validColumns);
            response.setTotalElements((long) reportModels.size());
            response.setTotalPages(ReportUtils.calculateTotalPages(reportModels.size(), request.getSize()));

            return response;
        } catch (Exception e) {
            log.error("[getProductDaily] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductsChartResponse getAllProductsChart(ProductChartRequest request) {
        try {
            // 1. Get chart column
            ColumnConfig columnChart = productColumnConfig.getColumnByCode(request.getChartData().getValue());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Get exchange rate
            BigDecimal lastExchangeRate = exchangeRateService.getLastExchangeRate(claimUtil.getCompanyId(), CurrencyCodeEnum.VND.name(), request.getCurrency().name(), request.getStartDate(), request.getEndDate());

            // 4. Inquiry product with order
            List<OrderQueryByProductModel> orderQueryModels = orderCustomRepository.reportOrderByProduct(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    List.of(), List.of(),
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Exchange rate
            applyExchangeRatesToOrders(orderQueryModels, lastExchangeRate, request.getCurrency());

            // 6. TODO: get ad cost if needed

            // 7. Combine order and ads cost
            List<ReportByProductModel> allProducts = combineDataReportProducts(orderQueryModels);

            // 8. Order by columnChart DESC and limit top
            List<ReportByProductModel> topProducts = sortAndGetTop(allProducts, columnChart.getCode(), request.getLimit());

            // 9. Calculate percent and map to response
            return buildProductsChartResponse(topProducts, allProducts, columnChart, request);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getAllProductsChart] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductChartResponse getProductChart(String productId, ProductChartRequest request) {
        try {
            // 1. Get chart column
            ColumnConfig columnChart = productColumnConfig.getColumnByCode(request.getChartData().getValue());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Fetch order data by date for this user
            List<OrderQueryByDateModel> orderQueryByDate = orderCustomRepository.reportOrderByDateOfProduct(
                    claimUtil.getDepartmentId(),
                    productId,
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting.getConfirmOrderWhen(),
                    reportSetting.getReturnOrderWhen()
            );

            // 4. Exchange revenue
            reportHelperService.exchangeRevenue(orderQueryByDate, request.getCurrency(), request.getStartDate(), request.getEndDate());

            // 5. TODO: get ad cost if needed

            // 6. Combine data into date models
            List<ReportByDateModel> reportByDateModels = combineDataReportProductDate(
                    orderQueryByDate,
                    request.getStartDate(),
                    request.getEndDate()
            );

            // 7. Build per-date data list and summary
            return buildSingleProductChartResponse(reportByDateModels, columnChart, request.getLimit());
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getProductChart] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private List<ReportByProductModel> combineDataReportProducts(List<OrderQueryByProductModel> orderQueryModels) {
        List<ReportByProductModel> reportModels = new ArrayList<>();

        // Combine all products from both order and ads data
        for (OrderQueryByProductModel orderQuery : orderQueryModels) {
            String productId = orderQuery.getProductId();

            // TODO: code ad cost
            BigDecimal adCost = BigDecimal.ZERO;

            ReportByProductModel reportModel = buildReportModelForProduct(orderQuery, adCost);
            reportModels.add(reportModel);
        }

        return reportModels;
    }

    private ReportByProductModel buildReportModelForProduct(OrderQueryByProductModel orderQuery, BigDecimal adCost) {
        ReportByProductModel reportModel = new ReportByProductModel();

        // Copy product info
        reportModel.setProductId(orderQuery.getProductId());
        reportModel.setName(orderQuery.getName());
        reportModel.setImage(orderQuery.getImage());

        // Copy order data
        reportModel.setOrders(orderQuery.getOrders());
        reportModel.setConfirmedOrders(orderQuery.getConfirmedOrders());
        reportModel.setReturnedOrders(orderQuery.getReturnedOrders());
        reportModel.setSuccessOrders(orderQuery.getSuccessOrders());
        reportModel.setRevenue(orderQuery.getRevenue());
        reportModel.setTrueRevenue(orderQuery.getTrueRevenue());

        // Set ads cost
        reportModel.setAdCost(adCost);

        // Calculate derived metrics
        calculateDerivedMetrics(reportModel);

        return reportModel;
    }

    private void calculateDerivedMetrics(ReportByProductModel reportModel) {
        BigDecimal adCost = reportModel.getAdCost() != null ? reportModel.getAdCost() : BigDecimal.ZERO;
        BigDecimal trueRevenue = reportModel.getTrueRevenue() != null ? reportModel.getTrueRevenue() : BigDecimal.ZERO;
        BigDecimal revenue = reportModel.getRevenue() != null ? reportModel.getRevenue() : BigDecimal.ZERO;

        // Calculate profit = trueRevenue - adCost
        BigDecimal profit = trueRevenue.subtract(adCost);
        reportModel.setProfit(profit);

        // Calculate adCostPerOrder
        reportModel.setAdCostPerOrder(reportHelperService.calculateAdCostPerOrder(adCost, reportModel.getOrders()));

        // Calculate adCostPerConfirmedOrder
        reportModel.setAdCostPerConfirmedOrder(reportHelperService.calculateAdCostPerOrder(adCost, reportModel.getConfirmedOrders()));

        // Calculate adCostPerRevenue
        reportModel.setAdCostPerRevenue(reportHelperService.calculateAdCostPerRevenue(adCost, revenue));
    }

    private void calculateDerivedMetricsProductDate(ReportByDateModel reportModel, BigDecimal adCost) {
        // Calculate profit = trueRevenue - adCost
        BigDecimal profit = reportModel.getTrueRevenue().subtract(adCost);
        reportModel.setProfit(profit);

        // Calculate adCostPerOrder
        reportModel.setAdCostPerOrder(reportHelperService.calculateAdCostPerOrder(adCost, reportModel.getOrders()));

        // Calculate adCostPerConfirmedOrder
        reportModel.setAdCostPerConfirmedOrder(reportHelperService.calculateAdCostPerOrder(adCost, reportModel.getConfirmedOrders()));

        // Calculate adCostPerRevenue
        reportModel.setAdCostPerRevenue(reportHelperService.calculateAdCostPerRevenue(adCost, reportModel.getRevenue()));
    }

    private ProductSummaryResponse buildProductSummaryResponse(List<ReportByProductModel> pagedReportModels, List<ReportByProductModel> allReportModels, List<ColumnRequest> columns) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(columns);
        List<ProductSummaryResponse.DataItem> dataItems = buildDataItems(pagedReportModels, columnConfigs);
        Map<String, Object> summary = reportHelperService.buildSummary(allReportModels, columnConfigs);

        ProductSummaryResponse response = new ProductSummaryResponse();
        response.setData(dataItems);
        response.setSummary(summary);

        return response;
    }

    private ProductDailyResponse buildReportProductDailyResponse(List<ReportByDateModel> pagedReportModels, List<ReportByDateModel> allReportModels, List<ColumnRequest> validColumns) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(validColumns);
        List<ProductDailyResponse.DataItem> dataItems = buildProductDailyDataItems(pagedReportModels, columnConfigs);
        Map<String, Object> summary = reportHelperService.buildSummary(allReportModels, columnConfigs);

        ProductDailyResponse response = new ProductDailyResponse();
        response.setData(dataItems);
        response.setSummary(summary);

        return response;
    }

    private ProductsChartResponse buildProductsChartResponse(List<ReportByProductModel> topProducts, List<ReportByProductModel> allProducts, ColumnConfig columnChart, ProductChartRequest request) {
        // Calculate total value from ALL products for percentage calculation
        BigDecimal totalValue = calculateTotalValue(allProducts, columnChart.getMapping());

        // Calculate total value from top products
        BigDecimal topTotalValue = calculateTotalValue(topProducts, columnChart.getMapping());

        List<ProductsChartResponse.ProductData> chartDataList = new ArrayList<>();

        // Add top products
        for (ReportByProductModel productModel : topProducts) {
            ProductsChartResponse.ProductData chartData = new ProductsChartResponse.ProductData();

            // Set product info
            ProductsChartResponse.Product productData = new ProductsChartResponse.Product();
            productData.setId(productModel.getProductId());
            productData.setName(productModel.getName());
            chartData.setProduct(productData);

            // Set data value
            Object valueObj = reportHelperService.getValueFromReportModel(productModel, columnChart.getCode());
            BigDecimal value = convertToBigDecimal(valueObj);
            BigDecimal percent = calculatePercent(value, totalValue);

            ProductsChartResponse.DataValue dataValue = new ProductsChartResponse.DataValue();
            dataValue.setValue(value);
            dataValue.setPercent(percent);
            chartData.setData(dataValue);

            // Set date values if splitByDate is true
            if (request.isSplitByDate()) {
                List<ProductsChartResponse.DateValue> dateValues = fetchDateValuesForProduct(
                        productModel.getProductId(),
                        request,
                        columnChart
                );
                chartData.setDateValues(dateValues);
            }

            chartDataList.add(chartData);
        }

        // Add "Other" product if top total is less than overall total
        if (topTotalValue.compareTo(totalValue) < 0) {
            BigDecimal otherValue = totalValue.subtract(topTotalValue);
            BigDecimal otherPercent = calculatePercent(otherValue, totalValue);

            ProductsChartResponse.ProductData otherChartData = new ProductsChartResponse.ProductData();

            // Set "Other" product info
            ProductsChartResponse.Product otherProductData = new ProductsChartResponse.Product();
            otherProductData.setId(null);
            otherProductData.setName("Other");
            otherChartData.setProduct(otherProductData);

            // Set data value for "Other"
            ProductsChartResponse.DataValue otherDataValue = new ProductsChartResponse.DataValue();
            otherDataValue.setValue(otherValue);
            otherDataValue.setPercent(otherPercent);
            otherChartData.setData(otherDataValue);

            // No date values for "Other" product
            otherChartData.setDateValues(null);

            chartDataList.add(otherChartData);
        }

        // Build summary
        ProductsChartResponse.Summary summary = new ProductsChartResponse.Summary();
        summary.setValue(totalValue);
        summary.setPercent(BigDecimal.valueOf(100.0));

        return ProductsChartResponse.builder()
                .totalElements(chartDataList.size())
                .productData(chartDataList)
                .summary(summary)
                .build();
    }

    private ProductChartResponse buildSingleProductChartResponse(List<ReportByDateModel> reportByDateModels, ColumnConfig columnChart, Limit limit) {
        // Calculate total value from ALL dates for percentage calculation
        BigDecimal totalValue = reportByDateModels.stream()
                .map(model -> convertToBigDecimal(reportHelperService.getValueFromReportModel(model, columnChart.getCode())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sort by selected metric DESC and limit top
        List<ReportByDateModel> topDates = reportByDateModels.stream()
                .sorted((m1, m2) -> {
                    Object v1 = reportHelperService.getValueFromReportModel(m1, columnChart.getCode());
                    Object v2 = reportHelperService.getValueFromReportModel(m2, columnChart.getCode());
                    return compareValues(v2, v1); // DESC
                })
                .limit(limit.getValue())
                .toList();

        // Calculate total value from top dates
        BigDecimal topTotalValue = topDates.stream()
                .map(model -> convertToBigDecimal(reportHelperService.getValueFromReportModel(model, columnChart.getCode())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build date data with percent
        List<ProductChartResponse.DateData> dateData = new ArrayList<>();

        // Add top dates
        for (ReportByDateModel model : topDates) {
            BigDecimal value = convertToBigDecimal(reportHelperService.getValueFromReportModel(model, columnChart.getCode()));
            BigDecimal percent = calculatePercent(value, totalValue);

            ProductChartResponse.DateData item = new ProductChartResponse.DateData();
            item.setDate(LocalDate.parse(model.getReportDate(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            item.setValue(value);
            item.setPercent(percent);
            dateData.add(item);
        }

        // Add "Other" date if top dates total is less than overall total
        if (topTotalValue.compareTo(totalValue) < 0) {
            BigDecimal otherValue = totalValue.subtract(topTotalValue);
            BigDecimal otherPercent = calculatePercent(otherValue, totalValue);

            ProductChartResponse.DateData otherItem = new ProductChartResponse.DateData();
            otherItem.setDate(null); // date = null for "Other"
            otherItem.setValue(otherValue);
            otherItem.setPercent(otherPercent);
            dateData.add(otherItem);
        }

        // Summary
        ProductChartResponse.Summary summary = new ProductChartResponse.Summary();
        summary.setValue(totalValue);
        summary.setPercent(BigDecimal.valueOf(100.0));

        ProductChartResponse response = new ProductChartResponse();
        response.setTotalElements(dateData.size());
        response.setDateData(dateData);
        response.setSummary(summary);
        return response;
    }

    private List<ColumnConfig> getColumnConfigs(List<ColumnRequest> columns) {
        return columns.stream()
                .map(columnRequest -> productColumnConfig.getColumnByCode(columnRequest.getCode()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ProductSummaryResponse.DataItem> buildDataItems(List<ReportByProductModel> reportModels, List<ColumnConfig> columnConfigs) {
        List<ProductSummaryResponse.DataItem> dataItems = new ArrayList<>();

        for (ReportByProductModel reportModel : reportModels) {
            ProductSummaryResponse.DataItem dataItem = new ProductSummaryResponse.DataItem();

            // Set product data
            ProductSummaryResponse.ProductData productData = new ProductSummaryResponse.ProductData();
            productData.setId(reportModel.getProductId());
            productData.setName(reportModel.getName());
            productData.setImageUrl(reportModel.getImage());
            dataItem.setProduct(productData);

            // Set extra data
            dataItem.setExtraData(reportHelperService.buildExtraData(reportModel, columnConfigs));
            dataItems.add(dataItem);
        }

        return dataItems;
    }

    private List<ProductDailyResponse.DataItem> buildProductDailyDataItems(List<ReportByDateModel> reportModels, List<ColumnConfig> columnConfigs) {
        List<ProductDailyResponse.DataItem> dataItems = new ArrayList<>();

        for (ReportByDateModel reportModel : reportModels) {
            ProductDailyResponse.DataItem dataItem = new ProductDailyResponse.DataItem();
            dataItem.setDate(reportModel.getReportDate());
            dataItem.setExtraData(reportHelperService.buildExtraData(reportModel, columnConfigs));
            dataItems.add(dataItem);
        }

        return dataItems;
    }

    private List<OrderQueryByDateModel> fetchOrderQueryByProductDateModels(String productId, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, ReportSettingResponse reportSetting) {
        List<OrderQueryByDateModel> orderQueryModel;

        String departmentId = claimUtil.getDepartmentId();
        orderQueryModel = orderCustomRepository.reportOrderByDateOfProduct(
                departmentId,
                productId,
                startDate,
                endDate,
                reportSetting.getConfirmOrderWhen(),
                reportSetting.getReturnOrderWhen()
        );

        reportHelperService.exchangeRevenue(orderQueryModel, currency, startDate, endDate);

        return orderQueryModel;
    }

    private List<ReportByDateModel> combineDataReportProductDate(List<OrderQueryByDateModel> orderQueryModels, LocalDate startDate, LocalDate endDate) {
        Map<String, OrderQueryByDateModel> orderMap = buildOrderMap(orderQueryModels);

        List<ReportByDateModel> reportModels = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateStr = DateUtils.dateToString(currentDate);
            ReportByDateModel reportModel = buildReportModelForDate(dateStr, orderMap);
            reportModels.add(reportModel);
            currentDate = currentDate.plusDays(1);
        }

        return reportModels;
    }

    private Map<String, OrderQueryByDateModel> buildOrderMap(List<OrderQueryByDateModel> orderQueryModels) {
        Map<String, OrderQueryByDateModel> orderMap = new HashMap<>();
        for (OrderQueryByDateModel orderQuery : orderQueryModels) {
            orderMap.put(orderQuery.getReportDate(), orderQuery);
        }
        return orderMap;
    }

    private ReportByDateModel buildReportModelForDate(String dateStr, Map<String, OrderQueryByDateModel> orderMap) {
        OrderQueryByDateModel orderQuery = orderMap.get(dateStr);

        // TODO: code ad cost
        BigDecimal adCost = BigDecimal.ZERO;

        ReportByDateModel reportModel = new ReportByDateModel();
        reportModel.setReportDate(dateStr);

        reportHelperService.populateOrderDataByDate(reportModel, orderQuery);
        reportModel.setAdCost(adCost);
        calculateDerivedMetricsProductDate(reportModel, adCost);

        return reportModel;
    }

    private void applyExchangeRatesToOrders(List<OrderQueryByProductModel> orderQueryModel, BigDecimal rate, CurrencyCodeEnum currency) {
        orderQueryModel.forEach(order -> {
            if (order.getRevenue() != null) {
                order.setRevenue(CurrencyUtils.exchange(currency, order.getRevenue(), rate));
            }
            if (order.getTrueRevenue() != null) {
                order.setTrueRevenue(CurrencyUtils.exchange(currency, order.getTrueRevenue(), rate));
            }
        });
    }

    private List<ReportByProductModel> sortAndGetTop(List<ReportByProductModel> reportModels, String code, Limit limit) {
        return reportModels.stream()
                .sorted((r1, r2) -> {
                    Object value1 = reportHelperService.getValueFromReportModel(r1, code);
                    Object value2 = reportHelperService.getValueFromReportModel(r2, code);
                    return compareValues(value2, value1); // DESC order
                })
                .limit(limit.getValue())
                .toList();
    }

    private int compareValues(Object value1, Object value2) {
        if (value1 instanceof BigDecimal bd1 && value2 instanceof BigDecimal bd2) {
            return bd1.compareTo(bd2);
        } else if (value1 instanceof Long l1 && value2 instanceof Long l2) {
            return l1.compareTo(l2);
        } else if (value1 instanceof Double d1 && value2 instanceof Double d2) {
            return d1.compareTo(d2);
        }
        return 0;
    }

    private BigDecimal calculateTotalValue(List<ReportByProductModel> reportModels, String mapping) {
        return reportModels.stream()
                .map(model -> {
                    Object value = reportHelperService.getValueFromReportModel(model, mapping);
                    return convertToBigDecimal(value);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePercent(BigDecimal value, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<ProductsChartResponse.DateValue> fetchDateValuesForProduct(String productId, ProductChartRequest request, ColumnConfig columnChart) {
        try {
            ReportSettingResponse reportSetting = configService.getConfig();

            // Fetch order data by date for this user
            List<OrderQueryByDateModel> orderQueryByDate = orderCustomRepository.reportOrderByDateOfProduct(
                    claimUtil.getDepartmentId(),
                    productId,
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting.getConfirmOrderWhen(),
                    reportSetting.getReturnOrderWhen()
            );

            // Exchange revenue
            reportHelperService.exchangeRevenue(orderQueryByDate, request.getCurrency(), request.getStartDate(), request.getEndDate());

            // TODO: get ad cost if needed

            // Combine data
            List<ReportByDateModel> reportByDateModels = combineDataReportProductDate(
                    orderQueryByDate,
                    request.getStartDate(),
                    request.getEndDate()
            );

            // Calculate total for percentage
            BigDecimal totalValueForUser = reportByDateModels.stream()
                    .map(model -> {
                        Object value = reportHelperService.getValueFromReportModel(model, columnChart.getCode());
                        return convertToBigDecimal(value);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Build date values
            List<ProductsChartResponse.DateValue> dateValues = new ArrayList<>();
            for (ReportByDateModel reportModel : reportByDateModels) {
                ProductsChartResponse.DateValue dateValue = new ProductsChartResponse.DateValue();
                dateValue.setDate(LocalDate.parse(reportModel.getReportDate(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                Object valueObj = reportHelperService.getValueFromReportModel(reportModel, columnChart.getCode());
                BigDecimal value = convertToBigDecimal(valueObj);
                BigDecimal percent = calculatePercent(value, totalValueForUser);

                dateValue.setValue(value);
                dateValue.setPercent(percent);
                dateValues.add(dateValue);
            }

            return dateValues;
        } catch (Exception e) {
            log.error("[fetchDateValuesForProduct] error for productId: {}, error: {}", productId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
