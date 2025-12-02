package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.configuration.ProductColumnConfig;
import com.nemi.report.constant.Limit;
import com.nemi.report.constant.OrderDirection;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.ads_manager.AdCostByDate;
import com.nemi.report.model.ads_manager.AdsCostOfProduct;
import com.nemi.report.model.ads_manager.AdsCostOfProductsRequest;
import com.nemi.report.model.ads_manager.AdsCostOfProductsResponse;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByProductModel;
import com.nemi.report.model.pojo.OrderSumModel;
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
import com.nemi.report.service.ProductReportService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ReportValidator;
import com.nemi.util.ClaimUtil;
import com.nemi.util.CurrencyUtils;
import com.nemi.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductReportServiceImpl implements ProductReportService {

    private final ExchangeRateService exchangeRateService;
    private final ClaimUtil claimUtil;
    private final ProductColumnConfig productColumnConfig;
    private final ConfigService configService;
    private final ReportHelperService reportHelperService;

    private final OrderCustomRepository orderCustomRepository;
    private final AdsManagerClient adsManagerClient;

    @Override
    public ProductSummaryResponse getProductSummary(ReportSummaryRequest request) {
        try {
            // 1. filter request
            PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), productColumnConfig.getListColumns());
            List<ColumnRequest> validOrders = ReportValidator.getValidOrders(request.getColumns(), productColumnConfig.getListColumns());
            List<FilterRequest> validFilters = ReportValidator.getValidFilters(request.getFilters(), productColumnConfig.getListColumns());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Get exchange rate
            BigDecimal lastExchangeRate = exchangeRateService.getLastExchangeRate(claimUtil.getCompanyId(), CurrencyCodeEnum.VND.name(), request.getCurrency().name(), request.getStartDate(), request.getEndDate());

            // 4. Inquiry product with order (paged)
            List<OrderQueryByProductModel> orderQueryModels = orderCustomRepository.reportOrderByProduct(claimUtil.getDepartmentId(), pageRequest,
                    request.getStartDate(), request.getEndDate(),
                    validOrders, validFilters,
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Get summary order (ALL products)
            OrderSumModel orderSumModel = orderCustomRepository.reportSumOrderByProduct(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    validOrders, validFilters,
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5.1 Count product list
            Long count = orderCustomRepository.reportCountOrderByProduct(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    validOrders, validFilters,
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 6. Exchange rate
            applyExchangeRatesToOrders(orderQueryModels, lastExchangeRate, request.getCurrency());
            applyExchangeRatesToOrderSum(orderSumModel, lastExchangeRate, request.getCurrency());

            // 7. Get ads cost if needed
            AdsCostOfProductsResponse adsCostOfProducts = null;
            if (containsSource(validColumns, ProductSource.ADS)) {
                List<String> productIds = orderQueryModels.stream().map(OrderQueryByProductModel::getProductId).toList();
                adsCostOfProducts = getAdsCostByProduct(request.getCurrency(), request.getStartDate(), request.getEndDate(), productIds);
            }

            // 8. Combine order and ads cost (paged models)
            List<ReportByProductModel> reportModels = combineDataReportProducts(orderQueryModels, adsCostOfProducts);

            // 9. Map to response
            ProductSummaryResponse response = buildProductSummaryResponse(reportModels, orderSumModel, adsCostOfProducts, validColumns);
            response.setTotalElements(count);
            response.setTotalPages(ReportUtils.calculateTotalPages(count, request.getSize()));
            return response;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getProductSummary] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public ProductDailyResponse getProductDaily(String productId, ReportSummaryRequest request) {
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

            // 3. Get ad cost if needed
            List<AdCostByDate> adCostByDates = new ArrayList<>();
            if (containsSource(request.getColumns(), ProductSource.ADS)) {
                adCostByDates = getAdsCostByProductDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), productId);
            }

            // 4. Combine data
            List<ReportByDateModel> reportModels = combineDataReportProductDate(orderQueryModel, adCostByDates, request.getStartDate(), request.getEndDate());

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
            PageRequest pageRequest = PageRequest.of(0, request.getLimit().getValue());
            List<ColumnRequest> orderColumns = List.of(ColumnRequest.builder().code(columnChart.getCode()).order(OrderDirection.DESC).build());
            List<OrderQueryByProductModel> orderQueryModels = orderCustomRepository.reportOrderByProduct(claimUtil.getDepartmentId(), pageRequest,
                    request.getStartDate(), request.getEndDate(),
                    orderColumns, List.of(),
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Get summary order (ALL products)
            OrderSumModel orderSumModel = orderCustomRepository.reportSumOrderByProduct(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    orderColumns, List.of(),
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Exchange rate
            applyExchangeRatesToOrders(orderQueryModels, lastExchangeRate, request.getCurrency());

            // 6. Get ad cost if needed
            AdsCostOfProductsResponse adsCostOfProducts = null;
            if (containsSource(List.of(columnChart), ProductSource.ADS)) {
                List<String> productIds = orderQueryModels.stream().map(OrderQueryByProductModel::getProductId).toList();
                adsCostOfProducts = getAdsCostByProduct(request.getCurrency(), request.getStartDate(), request.getEndDate(), productIds);
            }

            // 7. Combine order and ads cost
            List<ReportByProductModel> topProducts = combineDataReportProductsFromList(orderQueryModels, adsCostOfProducts);

            // 8. Calculate percent and map to response
            return buildProductsChartResponse(topProducts, orderSumModel, adsCostOfProducts, columnChart, request);
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

            // 3. Fetch order data by date for this product
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

            // 5. Get ad cost if needed
            List<AdCostByDate> adCostByDates = new ArrayList<>();
            if (containsSource(List.of(columnChart), ProductSource.ADS)) {
                adCostByDates = getAdsCostByProductDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), productId);
            }

            // 6. Combine data into date models
            List<ReportByDateModel> reportByDateModels = combineDataReportProductDate(
                    orderQueryByDate,
                    adCostByDates,
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

    // >>>>>>>>>>>>>>>>>>> PRIVATE METHOD >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

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

    private void applyExchangeRatesToOrderSum(OrderSumModel orderSumModel, BigDecimal rate, CurrencyCodeEnum currency) {
        if (orderSumModel.getRevenue() != null) {
            orderSumModel.setRevenue(CurrencyUtils.exchange(currency, orderSumModel.getRevenue(), rate));
        }
        if (orderSumModel.getTrueRevenue() != null) {
            orderSumModel.setTrueRevenue(CurrencyUtils.exchange(currency, orderSumModel.getTrueRevenue(), rate));
        }
    }

    private boolean containsSource(List<?> columns, ProductSource source) {
        return columns.stream()
                .anyMatch(c -> {
                    ColumnConfig column = (c instanceof ColumnConfig)
                            ? (ColumnConfig) c
                            : productColumnConfig.getColumnByCode(((ColumnRequest) c).getCode());
                    return Objects.nonNull(column) && source.equals(column.getSource());
                });
    }

    private AdsCostOfProductsResponse getAdsCostByProduct(CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, List<String> productIds) {
        AdsCostOfProductsRequest request = AdsCostOfProductsRequest.builder()
                .companyId(claimUtil.getCompanyId())
                .departmentId(claimUtil.getDepartmentId())
                .startDate(startDate)
                .endDate(endDate)
                .currency(currency.name())
                .productIds(productIds)
                .build();
        AdsCostOfProductsResponse response = adsManagerClient.getAdsCostOfProducts(request).getBody();
        if (Objects.isNull(response)) {
            log.error("[getAdsCostByProduct] response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
        return response;
    }

    // build helpers
    private List<ReportByProductModel> combineDataReportProductsFromList(List<OrderQueryByProductModel> orderQueryModels, AdsCostOfProductsResponse adsCostOfProducts) {
        Map<String, BigDecimal> adCostMap = new HashMap<>();
        if (adsCostOfProducts != null) {
            for (AdsCostOfProduct item : adsCostOfProducts.getAdsCostOfProducts()) {
                adCostMap.put(item.getProductId(), item.getSpent());
            }
        }

        List<ReportByProductModel> reportModels = new ArrayList<>();
        for (OrderQueryByProductModel order : orderQueryModels) {
            BigDecimal adCost = adCostMap.getOrDefault(order.getProductId(), BigDecimal.ZERO);
            ReportByProductModel model = getReportByProductModel(order, adCost);
            // derived metrics
            calculateDerivedMetrics(model);
            reportModels.add(model);
        }
        return reportModels;
    }

    private List<ReportByProductModel> combineDataReportProducts(List<OrderQueryByProductModel> orderQueryModels, AdsCostOfProductsResponse adsCostOfProducts) {
        Map<String, BigDecimal> adCostMap = new HashMap<>();
        if (adsCostOfProducts != null && adsCostOfProducts.getAdsCostOfProducts() != null) {
            for (AdsCostOfProduct item : adsCostOfProducts.getAdsCostOfProducts()) {
                adCostMap.put(item.getProductId(), item.getSpent());
            }
        }

        List<ReportByProductModel> reportModels = new ArrayList<>();
        for (OrderQueryByProductModel order : orderQueryModels) {
            BigDecimal adCost = adCostMap.getOrDefault(order.getProductId(), BigDecimal.ZERO);
            ReportByProductModel model = getReportByProductModel(order, adCost);
            // derived metrics
            calculateDerivedMetrics(model);
            reportModels.add(model);
        }
        return reportModels;
    }

    @NotNull
    private static ReportByProductModel getReportByProductModel(OrderQueryByProductModel order, BigDecimal adCost) {
        ReportByProductModel model = new ReportByProductModel();
        // product info
        model.setProductId(order.getProductId());
        model.setName(order.getName());
        model.setImage(order.getImage());
        // order data
        model.setOrders(order.getOrders());
        model.setConfirmedOrders(order.getConfirmedOrders());
        model.setReturnedOrders(order.getReturnedOrders());
        model.setSuccessOrders(order.getSuccessOrders());
        model.setRevenue(order.getRevenue());
        model.setTrueRevenue(order.getTrueRevenue());
        // ad cost
        model.setAdCost(adCost);
        return model;
    }

    private void calculateDerivedMetrics(ReportByProductModel model) {
        BigDecimal adCost = model.getAdCost() != null ? model.getAdCost() : BigDecimal.ZERO;
        BigDecimal trueRevenue = model.getTrueRevenue() != null ? model.getTrueRevenue() : BigDecimal.ZERO;
        BigDecimal revenue = model.getRevenue() != null ? model.getRevenue() : BigDecimal.ZERO;

        model.setProfit(trueRevenue.subtract(adCost));
        model.setAdCostPerOrder(reportHelperService.calculateAdCostPerOrder(adCost, model.getOrders()));
        model.setAdCostPerConfirmedOrder(reportHelperService.calculateAdCostPerOrder(adCost, model.getConfirmedOrders()));
        model.setAdCostPerRevenue(reportHelperService.calculateAdCostPerRevenue(adCost, revenue));
    }

    private ProductSummaryResponse buildProductSummaryResponse(List<ReportByProductModel> pagedReportModels,
                                                               OrderSumModel orderSumModel,
                                                               AdsCostOfProductsResponse adsCostOfProducts,
                                                               List<ColumnRequest> columns) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(columns);

        // Build data items for page
        List<ProductSummaryResponse.DataItem> dataItems = new ArrayList<>();
        for (ReportByProductModel model : pagedReportModels) {
            ProductSummaryResponse.DataItem item = new ProductSummaryResponse.DataItem();
            ProductSummaryResponse.ProductData productData = ProductSummaryResponse.ProductData.builder()
                    .id(model.getProductId())
                    .name(model.getName())
                    .imageUrl(model.getImage())
                    .build();
            item.setProduct(productData);
            item.setExtraData(reportHelperService.buildExtraData(model, columnConfigs));
            dataItems.add(item);
        }

        // Build summary from ALL products using orderSum and ads total
        ReportByProductModel totalModel = getReportByProductModel(orderSumModel, adsCostOfProducts);
        calculateDerivedMetrics(totalModel);

        Map<String, Object> summary = reportHelperService.buildSummary(List.of(totalModel), columnConfigs);

        return ProductSummaryResponse.builder()
                .data(dataItems)
                .summary(summary)
                .build();
    }

    @NotNull
    private static ReportByProductModel getReportByProductModel(OrderSumModel orderSumModel, AdsCostOfProductsResponse adsCostOfProducts) {
        ReportByProductModel totalModel = new ReportByProductModel();
        totalModel.setOrders(orderSumModel.getOrders());
        totalModel.setConfirmedOrders(orderSumModel.getConfirmedOrders());
        totalModel.setReturnedOrders(orderSumModel.getReturnedOrders());
        totalModel.setSuccessOrders(orderSumModel.getSuccessOrders());
        totalModel.setRevenue(orderSumModel.getRevenue());
        totalModel.setTrueRevenue(orderSumModel.getTrueRevenue());
        BigDecimal totalAdCost = (adsCostOfProducts != null && adsCostOfProducts.getTotalSpent() != null)
                ? adsCostOfProducts.getTotalSpent()
                : BigDecimal.ZERO;
        totalModel.setAdCost(totalAdCost);
        return totalModel;
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

    private List<AdCostByDate> getAdsCostByProductDate(CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, String userId) {
        AdsCostOfProductsRequest request = AdsCostOfProductsRequest.builder()
                .companyId(claimUtil.getCompanyId())
                .departmentId(claimUtil.getDepartmentId())
                .startDate(startDate)
                .endDate(endDate)
                .currency(currency.name())
                .productIds(List.of(userId))
                .build();
        AdsCostOfProductsResponse response = adsManagerClient.getAdsCostOfProducts(request).getBody();
        if (Objects.isNull(response) || response.getAdsCostOfProducts().isEmpty()) {
            log.error("[getAdsCostByUserDate] response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
        return response.getAdsCostOfProducts().get(0).getAdCostByDate();
    }

    private List<ReportByDateModel> combineDataReportProductDate(List<OrderQueryByDateModel> orderQueryModels,  List<AdCostByDate> adCostByDates, LocalDate startDate, LocalDate endDate) {
        Map<String, OrderQueryByDateModel> orderMap = buildOrderMap(orderQueryModels);
        Map<String, BigDecimal> adCostMap = buildAdCostMapByDate(adCostByDates);

        List<ReportByDateModel> reportModels = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateStr = DateUtils.dateToString(currentDate);
            ReportByDateModel reportModel = buildReportModelForDate(dateStr, orderMap, adCostMap);
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

    private ReportByDateModel buildReportModelForDate(String dateStr, Map<String, OrderQueryByDateModel> orderMap, Map<String, BigDecimal> adCostMap) {
        OrderQueryByDateModel orderQuery = orderMap.get(dateStr);
        BigDecimal adCost = adCostMap.getOrDefault(dateStr, BigDecimal.ZERO);

        ReportByDateModel reportModel = new ReportByDateModel();
        reportModel.setReportDate(dateStr);

        reportHelperService.populateOrderDataByDate(reportModel, orderQuery);
        reportModel.setAdCost(adCost);
        calculateDerivedMetricsUserDate(reportModel, adCost);

        return reportModel;
    }

    private void calculateDerivedMetricsUserDate(ReportByDateModel reportModel, BigDecimal adCost) {
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

    private Map<String, BigDecimal> buildAdCostMapByDate(List<AdCostByDate> adCostByDates) {
        Map<String, BigDecimal> adCostMap = new HashMap<>();
        for (AdCostByDate adCost : adCostByDates) {
            adCostMap.put(adCost.getReportDate(), adCost.getSpent());
        }
        return adCostMap;
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

    private List<ColumnConfig> getColumnConfigs(List<ColumnRequest> columns) {
        return columns.stream()
                .map(columnRequest -> productColumnConfig.getColumnByCode(columnRequest.getCode()))
                .filter(Objects::nonNull)
                .toList();
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

    private ProductsChartResponse buildProductsChartResponse(List<ReportByProductModel> topProducts, OrderSumModel orderSumModel, AdsCostOfProductsResponse adsCostOfProducts, ColumnConfig columnChart, ProductChartRequest request) {
        // Get total value from OrderSumModel and ads cost based on columnChart
        ReportByProductModel totalModel = getReportByProductModel(orderSumModel, adsCostOfProducts);
        calculateDerivedMetrics(totalModel);
        Object totalValueObj = reportHelperService.getValueFromReportModel(totalModel, columnChart.getCode());
        BigDecimal totalValue = convertToBigDecimal(totalValueObj);

        // Calculate total value from top products
        BigDecimal topTotalValue = calculateTotalValue(topProducts, columnChart.getMapping());

        List<ProductsChartResponse.ProductData> chartDataList = new ArrayList<>();

        // Add top products
        for (ReportByProductModel productModel : topProducts) {
            ProductsChartResponse.ProductData chartData = new ProductsChartResponse.ProductData();

            // Set product info
            ProductsChartResponse.Product product = new ProductsChartResponse.Product();
            product.setId(productModel.getProductId());
            product.setName(productModel.getName());
            chartData.setProduct(product);

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
            ProductsChartResponse.Product otherProduct = new ProductsChartResponse.Product();
            otherProduct.setId(null);
            otherProduct.setName("Other");
            otherChartData.setProduct(otherProduct);

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

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value);
        } else if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value);
        } else if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        }
        return BigDecimal.ZERO;
    }

    private List<ProductsChartResponse.DateValue> fetchDateValuesForProduct(String productId, ProductChartRequest request, ColumnConfig columnChart) {
        try {
            ReportSettingResponse reportSetting = configService.getConfig();

            // Fetch order data by date for this product
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

            // Get ads cost by date if needed
            List<AdCostByDate> adCostByDates = new ArrayList<>();
            if (ProductSource.ADS.equals(columnChart.getSource())) {
                adCostByDates = getAdsCostByProductDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), productId);
            }

            // Combine data
            List<ReportByDateModel> reportByDateModels = combineDataReportProductDate(
                    orderQueryByDate,
                    adCostByDates,
                    request.getStartDate(),
                    request.getEndDate()
            );

            // Calculate total for percentage
            BigDecimal totalValueForProduct = reportByDateModels.stream()
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
                BigDecimal percent = calculatePercent(value, totalValueForProduct);

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
                    return ReportUtils.compareValues(v2, v1); // DESC
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
}
