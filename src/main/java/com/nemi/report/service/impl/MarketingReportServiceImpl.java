package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.client.SystemManagerClient;
import com.nemi.report.configuration.MarketingColumnConfig;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.ads_manager.AdCostByDate;
import com.nemi.report.model.ads_manager.AdsCostOfDepartmentRequest;
import com.nemi.report.model.ads_manager.AdsCostResponse;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.OrderQueryModel;
import com.nemi.report.model.pojo.ReportModel;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.marketing.MarketingChartRequest;
import com.nemi.report.model.response.marketing.MarketingChartResponse;
import com.nemi.report.model.response.marketing.MarketingDataItem;
import com.nemi.report.model.response.marketing.MarketingReportResponse;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.model.system_manager.ExchangeRateResponse;
import com.nemi.report.repository.OrderCustomRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.MarketingReportService;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketingReportServiceImpl implements MarketingReportService {

    private static final int DECIMAL_SCALE = 2;
    private static final int REVENUE_DECIMAL_SCALE = 4;
    private static final String SUMMARY_TYPE_SUM = "SUM";

    private final ClaimUtil claimUtil;
    private final ConfigService configService;
    private final AdsManagerClient adsManagerClient;
    private final MarketingColumnConfig marketingColumnConfig;
    private final OrderCustomRepository orderCustomRepository;
    private final SystemManagerClient systemManagerClient;

    @Override
    public MarketingReportResponse getMarketingReportSummary(ReportSummaryRequest request) {
        try {
            ReportSettingResponse reportSetting = configService.getConfig();

            List<OrderQueryModel> orderQueryModel = fetchOrderQueryModels(
                    request,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting
            );

            List<AdCostByDate> adCostByDate = fetchAdsCostIfNeeded(
                    request,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            List<ReportModel> reportModels = combineData(orderQueryModel, adCostByDate, request.getStartDate(), request.getEndDate());

            // Pagination
            List<ReportModel> pagedModels = paginateReportModels(reportModels, request.getPage(), request.getSize());

            // Build response
            MarketingReportResponse response = buildReportSummaryResponse(pagedModels, reportModels, request);
            response.setTotalElements((long) reportModels.size());
            response.setTotalPages(calculateTotalPages(reportModels.size(), request.getSize()));
            return response;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getMarketingReportSummary] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public MarketingChartResponse getMarketingChart(MarketingChartRequest request) {
        try {
            ReportSettingResponse reportSetting = configService.getConfig();
            ColumnConfig columnChart = marketingColumnConfig.getColumnByCode(request.getChartData().getValue());

            List<OrderQueryModel> orderQueryModel = fetchOrderQueryModels(
                    columnChart,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting
            );

            List<AdCostByDate> adCostByDate = fetchAdsCostIfNeeded(
                    columnChart,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate()
            );

            List<ReportModel> reportModels = combineData(orderQueryModel, adCostByDate, request.getStartDate(), request.getEndDate());

            return buildMarketingChartResponse(reportModels, columnChart);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getMarketingChart] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private List<OrderQueryModel> fetchOrderQueryModels(Object requestOrColumn, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, ReportSettingResponse reportSetting) {
        List<OrderQueryModel> orderQueryModel = new ArrayList<>();

        boolean needsOrderData = (requestOrColumn instanceof ReportSummaryRequest)
                ? isContainOrder((ReportSummaryRequest) requestOrColumn)
                : isContainOrder((ColumnConfig) requestOrColumn);

        if (needsOrderData) {
            String departmentId = claimUtil.getDepartmentId();
            orderQueryModel = orderCustomRepository.reportOrderOfDepartmentByDate(
                    departmentId,
                    startDate,
                    endDate,
                    reportSetting.getConfirmOrderWhen(),
                    reportSetting.getReturnOrderWhen()
            );

            exchangeRevenue(orderQueryModel, currency, startDate, endDate);
        }

        return orderQueryModel;
    }

    private List<AdCostByDate> fetchAdsCostIfNeeded(Object requestOrColumn, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate) {
        List<AdCostByDate> adCostByDate = new ArrayList<>();

        boolean needsAdsCost = (requestOrColumn instanceof ReportSummaryRequest)
                ? isContainAdsCost((ReportSummaryRequest) requestOrColumn)
                : isContainAdsCost((ColumnConfig) requestOrColumn);

        if (needsAdsCost) {
            adCostByDate = getAdsCostByDate(currency, startDate, endDate);
        }

        return adCostByDate;
    }

    private List<ReportModel> paginateReportModels(List<ReportModel> reportModels, int page, int size) {
        int totalElements = reportModels.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        return reportModels.subList(fromIndex, toIndex);
    }

    private int calculateTotalPages(int totalElements, int size) {
        return (int) Math.ceil(totalElements / (double) size);
    }

    private void exchangeRevenue(List<OrderQueryModel> orderQueryModel, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate) {
        ExchangeRateResponse exchangeRateResponse = getExchangeRate(
                claimUtil.getCompanyId(),
                CurrencyCodeEnum.VND.name(),
                currency.name(),
                startDate,
                endDate
        );

        validateExchangeRateResponse(exchangeRateResponse);
        Map<String, BigDecimal> rateByDateMap = buildExchangeRateMap(exchangeRateResponse);
        applyExchangeRatesToOrders(orderQueryModel, rateByDateMap, currency);
    }

    private void validateExchangeRateResponse(ExchangeRateResponse exchangeRateResponse) {
        if (exchangeRateResponse == null || exchangeRateResponse.getRatesByDate() == null) {
            log.error("[exchangeRevenue] Exchange rate response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private Map<String, BigDecimal> buildExchangeRateMap(ExchangeRateResponse exchangeRateResponse) {
        Map<String, BigDecimal> rateByDateMap = new HashMap<>();
        for (var rateByDate : exchangeRateResponse.getRatesByDate()) {
            String dateStr = DateUtils.dateToString(rateByDate.getDate());
            rateByDateMap.put(dateStr, rateByDate.getRate());
        }
        return rateByDateMap;
    }

    private void applyExchangeRatesToOrders(List<OrderQueryModel> orderQueryModel, Map<String, BigDecimal> rateByDateMap, CurrencyCodeEnum currency) {
        orderQueryModel.forEach(order -> {
            BigDecimal rate = rateByDateMap.getOrDefault(order.getReportDate(), BigDecimal.ONE);

            if (order.getRevenue() != null) {
                order.setRevenue(CurrencyUtils.exchange(currency, order.getRevenue(), rate));
            }
            if (order.getTrueRevenue() != null) {
                order.setTrueRevenue(CurrencyUtils.exchange(currency, order.getTrueRevenue(), rate));
            }
        });
    }

    private boolean isContainAdsCost(ReportSummaryRequest request) {
        return containsSource(request.getColumns(), ProductSource.ADS);
    }

    private boolean isContainAdsCost(ColumnConfig column) {
        return ProductSource.ADS.equals(column.getSource());
    }

    private boolean isContainOrder(ReportSummaryRequest request) {
        return containsSource(request.getColumns(), ProductSource.ORDER);
    }

    private boolean isContainOrder(ColumnConfig column) {
        return ProductSource.ORDER.equals(column.getSource());
    }

    private boolean containsSource(List<?> columns, ProductSource source) {
        return columns.stream()
                .anyMatch(c -> {
                    ColumnConfig column = (c instanceof ColumnConfig)
                            ? (ColumnConfig) c
                            : marketingColumnConfig.getColumnByCode(((com.nemi.report.model.request.ColumnRequest) c).getCode());
                    return Objects.nonNull(column) && source.equals(column.getSource());
                });
    }

    private List<AdCostByDate> getAdsCostByDate(CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate) {
        AdsCostOfDepartmentRequest adsCostOfDepartmentRequest = AdsCostOfDepartmentRequest.builder()
                .companyId(claimUtil.getCompanyId())
                .departmentId(claimUtil.getDepartmentId())
                .startDate(startDate)
                .endDate(endDate)
                .currency(currency.name())
                .build();
        AdsCostResponse response = adsManagerClient.getAdsCostOfDepartment(adsCostOfDepartmentRequest).getBody();
        if (Objects.isNull(response)) {
            log.error("[getAdsCostByDate] response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
        return response.getAdCostByDate();
    }

    private List<ReportModel> combineData(List<OrderQueryModel> orderQueryModels, List<AdCostByDate> adCostByDates, LocalDate startDate, LocalDate endDate) {
        Map<String, OrderQueryModel> orderMap = buildOrderMap(orderQueryModels);
        Map<String, BigDecimal> adCostMap = buildAdCostMap(adCostByDates);

        List<ReportModel> reportModels = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dateStr = DateUtils.dateToString(currentDate);
            ReportModel reportModel = buildReportModelForDate(dateStr, orderMap, adCostMap);
            reportModels.add(reportModel);
            currentDate = currentDate.plusDays(1);
        }

        return reportModels;
    }

    private Map<String, OrderQueryModel> buildOrderMap(List<OrderQueryModel> orderQueryModels) {
        Map<String, OrderQueryModel> orderMap = new HashMap<>();
        for (OrderQueryModel orderQuery : orderQueryModels) {
            orderMap.put(orderQuery.getReportDate(), orderQuery);
        }
        return orderMap;
    }

    private Map<String, BigDecimal> buildAdCostMap(List<AdCostByDate> adCostByDates) {
        Map<String, BigDecimal> adCostMap = new HashMap<>();
        for (AdCostByDate adCost : adCostByDates) {
            adCostMap.put(adCost.getReportDate(), adCost.getSpent());
        }
        return adCostMap;
    }

    private ReportModel buildReportModelForDate(String dateStr, Map<String, OrderQueryModel> orderMap, Map<String, BigDecimal> adCostMap) {
        OrderQueryModel orderQuery = orderMap.get(dateStr);
        BigDecimal adCost = adCostMap.getOrDefault(dateStr, BigDecimal.ZERO);

        ReportModel reportModel = new ReportModel();
        reportModel.setReportDate(dateStr);

        populateOrderData(reportModel, orderQuery);
        reportModel.setAdCost(adCost);
        calculateDerivedMetrics(reportModel, adCost);

        return reportModel;
    }

    private void populateOrderData(ReportModel reportModel, OrderQueryModel orderQuery) {
        if (orderQuery != null) {
            reportModel.setOrders(orderQuery.getOrders());
            reportModel.setConfirmedOrders(orderQuery.getConfirmedOrders());
            reportModel.setReturnedOrders(orderQuery.getReturnedOrders());
            reportModel.setSuccessOrders(orderQuery.getSuccessOrders());
            reportModel.setRevenue(orderQuery.getRevenue());
            reportModel.setTrueRevenue(orderQuery.getTrueRevenue());
        } else {
            reportModel.setOrders(0L);
            reportModel.setConfirmedOrders(0L);
            reportModel.setReturnedOrders(0L);
            reportModel.setSuccessOrders(0L);
            reportModel.setRevenue(BigDecimal.ZERO);
            reportModel.setTrueRevenue(BigDecimal.ZERO);
        }
    }

    private void calculateDerivedMetrics(ReportModel reportModel, BigDecimal adCost) {
        // Calculate profit = trueRevenue - adCost
        BigDecimal profit = reportModel.getTrueRevenue().subtract(adCost);
        reportModel.setProfit(profit);

        // Calculate adCostPerOrder
        reportModel.setAdCostPerOrder(calculateAdCostPerOrder(adCost, reportModel.getOrders()));

        // Calculate adCostPerConfirmedOrder
        reportModel.setAdCostPerConfirmedOrder(calculateAdCostPerOrder(adCost, reportModel.getConfirmedOrders()));

        // Calculate adCostPerRevenue
        reportModel.setAdCostPerRevenue(calculateAdCostPerRevenue(adCost, reportModel.getRevenue()));
    }

    private BigDecimal calculateAdCostPerOrder(BigDecimal adCost, Long orders) {
        if (orders > 0) {
            return adCost.divide(BigDecimal.valueOf(orders), DECIMAL_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private Double calculateAdCostPerRevenue(BigDecimal adCost, BigDecimal revenue) {
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            return adCost.divide(revenue, REVENUE_DECIMAL_SCALE, RoundingMode.HALF_UP).doubleValue();
        }
        return 0.0;
    }

    private MarketingReportResponse buildReportSummaryResponse(List<ReportModel> pagedReportModels, List<ReportModel> allReportModels, ReportSummaryRequest request) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(request);
        List<MarketingDataItem> dataItems = buildDataItems(pagedReportModels, columnConfigs);
        Map<String, Object> summary = buildSummary(allReportModels, columnConfigs);

        MarketingReportResponse response = new MarketingReportResponse();
        response.setData(dataItems);
        response.setSummary(summary);

        return response;
    }

    private List<ColumnConfig> getColumnConfigs(ReportSummaryRequest request) {
        return request.getColumns().stream()
                .map(columnRequest -> marketingColumnConfig.getColumnByCode(columnRequest.getCode()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<MarketingDataItem> buildDataItems(List<ReportModel> reportModels, List<ColumnConfig> columnConfigs) {
        List<MarketingDataItem> dataItems = new ArrayList<>();

        for (ReportModel reportModel : reportModels) {
            MarketingDataItem dataItem = new MarketingDataItem();
            dataItem.setDate(reportModel.getReportDate());
            dataItem.setExtraData(buildExtraData(reportModel, columnConfigs));
            dataItems.add(dataItem);
        }

        return dataItems;
    }

    private Map<String, Object> buildExtraData(ReportModel reportModel, List<ColumnConfig> columnConfigs) {
        Map<String, Object> extraData = new HashMap<>();

        for (ColumnConfig columnConfig : columnConfigs) {
            String code = columnConfig.getCode();
            String mapping = columnConfig.getMapping();
            Object value = getValueFromReportModel(reportModel, mapping);
            extraData.put(code, value);
        }

        return extraData;
    }

    private MarketingChartResponse buildMarketingChartResponse(List<ReportModel> reportModels, ColumnConfig columnConfig) {
        List<ColumnConfig> columnConfigs = List.of(columnConfig);
        List<MarketingDataItem> dataItems = buildDataItems(reportModels, columnConfigs);

        return MarketingChartResponse.builder()
                .data(dataItems)
                .build();
    }

    private Object getValueFromReportModel(ReportModel reportModel, String mapping) {
        if (mapping == null) {
            return null;
        }

        return switch (mapping) {
            case "orders" -> reportModel.getOrders();
            case "confirmed_orders" -> reportModel.getConfirmedOrders();
            case "returned_orders" -> reportModel.getReturnedOrders();
            case "success_orders" -> reportModel.getSuccessOrders();
            case "revenue" -> reportModel.getRevenue();
            case "true_revenue" -> reportModel.getTrueRevenue();
            case "profit" -> reportModel.getProfit();
            case "ad_cost" -> reportModel.getAdCost();
            case "ad_cost_per_order" -> reportModel.getAdCostPerOrder();
            case "ads_cost_per_confirmed_order" -> reportModel.getAdCostPerConfirmedOrder();
            case "ad_cost_per_revenue" -> reportModel.getAdCostPerRevenue();
            default -> null;
        };
    }

    private Map<String, Object> buildSummary(List<ReportModel> reportModels, List<ColumnConfig> columnConfigs) {
        Map<String, Object> summary = new HashMap<>();

        for (ColumnConfig columnConfig : columnConfigs) {
            String code = columnConfig.getCode();
            String mapping = columnConfig.getMapping();

            if (mapping == null) {
                continue;
            }

            // Calculate summary based on summary type (SUM or AVG)
            Object summaryValue;
            if (columnConfig.getSummaryType() == null || columnConfig.getSummaryType().toString().equals(SUMMARY_TYPE_SUM)) {
                summaryValue = calculateSum(reportModels, mapping);
            } else {
                summaryValue = calculateAverage(reportModels, mapping);
            }

            summary.put(code, summaryValue);
        }

        return summary;
    }

    private Object calculateSum(List<ReportModel> reportModels, String mapping) {
        return switch (mapping) {
            case "orders" -> reportModels.stream().mapToLong(ReportModel::getOrders).sum();
            case "confirmed_orders" -> reportModels.stream().mapToLong(ReportModel::getConfirmedOrders).sum();
            case "returned_orders" -> reportModels.stream().mapToLong(ReportModel::getReturnedOrders).sum();
            case "success_orders" -> reportModels.stream().mapToLong(ReportModel::getSuccessOrders).sum();
            case "revenue" -> reportModels.stream()
                    .map(ReportModel::getRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "true_revenue" -> reportModels.stream()
                    .map(ReportModel::getTrueRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "profit" -> reportModels.stream()
                    .map(ReportModel::getProfit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "ad_cost" -> reportModels.stream()
                    .map(ReportModel::getAdCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            default -> null;
        };
    }

    private Object calculateAverage(List<ReportModel> reportModels, String mapping) {
        if (reportModels.isEmpty()) {
            return 0.0;
        }

        return switch (mapping) {
            case "ad_cost_per_order" -> {
                BigDecimal sum = reportModels.stream()
                        .map(ReportModel::getAdCostPerOrder)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(reportModels.size()), DECIMAL_SCALE, RoundingMode.HALF_UP);
            }
            case "ads_cost_per_confirmed_order" -> {
                BigDecimal sum = reportModels.stream()
                        .map(ReportModel::getAdCostPerConfirmedOrder)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(reportModels.size()), DECIMAL_SCALE, RoundingMode.HALF_UP);
            }
            case "ad_cost_per_revenue" -> {
                double sum = reportModels.stream()
                        .mapToDouble(ReportModel::getAdCostPerRevenue)
                        .sum();
                yield sum / reportModels.size();
            }
            default -> null;
        };
    }

    private ExchangeRateResponse getExchangeRate(Integer companyId, String currentCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate) {
        String startDateStr = startDate.toString();
        String endDateStr = endDate.toString();

        return systemManagerClient.getExchangeRate(
                companyId, currentCurrency, targetCurrency, startDateStr, endDateStr).getBody();
    }
}
