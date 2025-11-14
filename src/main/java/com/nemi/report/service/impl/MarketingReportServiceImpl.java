package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
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
import com.nemi.report.model.response.ReportSummaryResponse;
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

    private final ClaimUtil claimUtil;
    private final ConfigService configService;
    private final AdsManagerClient adsManagerClient;
    private final MarketingColumnConfig marketingColumnConfig;

    private final OrderCustomRepository orderCustomRepository;
    private final SystemManagerClient systemManagerClient;

    @Override
    public ReportSummaryResponse getMarketingReportSummary(ReportSummaryRequest request) {
        String departmentId = claimUtil.getDepartmentId();

        try {
            ReportSettingResponse reportSetting = configService.getConfig();

            // 1. Get summary of orders
            List<OrderQueryModel> orderQueryModel = new ArrayList<>();
            if (isContainOrder(request)) {
                orderQueryModel = orderCustomRepository.reportOrderOfDepartmentByDate(departmentId, request.getStartDate(), request.getEndDate(), reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

                // Exchange revenue if currency is not VND
                if (!CurrencyCodeEnum.VND.equals(request.getCurrency())) {
                    exchangeRevenue(orderQueryModel, request);
                }
            }

            // 2. Get ads cost if needed
            List<AdCostByDate> adCostByDate = new ArrayList<>();
            if (isContainAdsCost(request)) {
                adCostByDate = getAdsCostByDate(request);
            }

            // 3. Combine data
            List<ReportModel> reportModels = combineData(orderQueryModel, adCostByDate, request.getStartDate(), request.getEndDate());

            // 4. Build response
            return buildReportSummaryResponse(reportModels, request);
        } catch (Exception e) {
            log.error("[getMarketingReportSummary] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private void exchangeRevenue(List<OrderQueryModel> orderQueryModel, ReportSummaryRequest request) {
        ExchangeRateResponse exchangeRateResponse = getExchangeRate(
                claimUtil.getCompanyId(),
                CurrencyCodeEnum.VND.name(),
                request.getCurrency().name(),
                request.getStartDate(),
                request.getEndDate()
        );

        if (exchangeRateResponse == null || exchangeRateResponse.getRatesByDate() == null) {
            log.error("[exchangeRevenue] Exchange rate response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }

        // Create a map of rates by date for quick lookup
        Map<String, BigDecimal> rateByDateMap = new HashMap<>();
        for (var rateByDate : exchangeRateResponse.getRatesByDate()) {
            String dateStr = DateUtils.dateToString(rateByDate.getDate());
            rateByDateMap.put(dateStr, rateByDate.getRate());
        }

        // Exchange revenue and true revenue for each order
        orderQueryModel.forEach(order -> {
            // Get rate by date
            BigDecimal rate = rateByDateMap.getOrDefault(order.getReportDate(), BigDecimal.ONE);

            // Exchange revenue and true revenue
            if (order.getRevenue() != null) {
                order.setRevenue(CurrencyUtils.exchange(request.getCurrency(), order.getRevenue(), rate));
            }
            if (order.getTrueRevenue() != null) {
                order.setTrueRevenue(CurrencyUtils.exchange(request.getCurrency(), order.getTrueRevenue(), rate));
            }
        });
    }

    private boolean isContainAdsCost(ReportSummaryRequest request) {
        return request.getColumns().stream()
                .anyMatch(c -> {
                    ColumnConfig column = marketingColumnConfig.getColumnByCode(c.getCode());
                    return Objects.nonNull(column) && ProductSource.ADS.equals(column.getSource());
                });
    }

    private boolean isContainOrder(ReportSummaryRequest request) {
        return request.getColumns().stream()
                .anyMatch(c -> {
                    ColumnConfig column = marketingColumnConfig.getColumnByCode(c.getCode());
                    return Objects.nonNull(column) && ProductSource.ORDER.equals(column.getSource());
                });
    }

    private List<AdCostByDate> getAdsCostByDate(ReportSummaryRequest request) {
        AdsCostOfDepartmentRequest adsCostOfDepartmentRequest = AdsCostOfDepartmentRequest.builder()
                .companyId(claimUtil.getCompanyId())
                .departmentId(claimUtil.getDepartmentId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .currency(request.getCurrency().name())
                .build();
        AdsCostResponse response = adsManagerClient.getAdsCostOfDepartment(adsCostOfDepartmentRequest).getBody();
        if (Objects.isNull(response)) {
            log.error("[getAdsCostByDate] response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
        return response.getAdCostByDate();
    }

    private List<ReportModel> combineData(List<OrderQueryModel> orderQueryModels, List<AdCostByDate> adCostByDates, LocalDate startDate, LocalDate endDate) {

        // Create maps for quick lookup by date
        Map<String, OrderQueryModel> orderMap = new HashMap<>();
        for (OrderQueryModel orderQuery : orderQueryModels) {
            orderMap.put(orderQuery.getReportDate(), orderQuery);
        }

        Map<String, BigDecimal> adCostMap = new HashMap<>();
        for (AdCostByDate adCost : adCostByDates) {
            adCostMap.put(adCost.getReportDate(), adCost.getSpent());
        }

        List<ReportModel> reportModels = new ArrayList<>();

        // Iterate through all days between startDate and endDate (inclusive)
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = DateUtils.dateToString(currentDate);

            // Get order data for this date, or create empty data
            OrderQueryModel orderQuery = orderMap.get(dateStr);

            ReportModel reportModel = new ReportModel();
            reportModel.setReportDate(dateStr);

            // Set order data (or zeros if not found)
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

            // Get ad cost for this date (or zero if not found)
            BigDecimal adCost = adCostMap.getOrDefault(dateStr, BigDecimal.ZERO);
            reportModel.setAdCost(adCost);

            // Calculate profit = trueRevenue - adCost
            BigDecimal profit = reportModel.getTrueRevenue().subtract(adCost);
            reportModel.setProfit(profit);

            // Calculate adCostPerOrder = adCost / orders (if orders > 0)
            if (reportModel.getOrders() > 0) {
                BigDecimal adCostPerOrder = adCost.divide(BigDecimal.valueOf(reportModel.getOrders()), 2, RoundingMode.HALF_UP);
                reportModel.setAdCostPerOrder(adCostPerOrder);
            } else {
                reportModel.setAdCostPerOrder(BigDecimal.ZERO);
            }

            // Calculate adCostPerConfirmedOrder = adCost / confirmedOrders (if confirmedOrders > 0)
            if (reportModel.getConfirmedOrders() > 0) {
                BigDecimal adCostPerConfirmedOrder = adCost.divide(BigDecimal.valueOf(reportModel.getConfirmedOrders()), 2, RoundingMode.HALF_UP);
                reportModel.setAdCostPerConfirmedOrder(adCostPerConfirmedOrder);
            } else {
                reportModel.setAdCostPerConfirmedOrder(BigDecimal.ZERO);
            }

            // Calculate adCostPerRevenue = adCost / revenue (if revenue > 0)
            if (reportModel.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal adCostPerRevenue = adCost.divide(reportModel.getRevenue(), 4, RoundingMode.HALF_UP);
                reportModel.setAdCostPerRevenue(adCostPerRevenue.doubleValue());
            } else {
                reportModel.setAdCostPerRevenue(0.0);
            }

            reportModels.add(reportModel);

            // Move to next day
            currentDate = currentDate.plusDays(1);
        }

        return reportModels;
    }

    private ReportSummaryResponse buildReportSummaryResponse(List<ReportModel> reportModels, ReportSummaryRequest request) {
        ReportSummaryResponse response = new ReportSummaryResponse();
        List<ReportSummaryResponse.DataItem> dataItems = new ArrayList<>();

        // Get column configurations
        List<ColumnConfig> columnConfigs = request.getColumns().stream()
                .map(columnRequest -> marketingColumnConfig.getColumnByCode(columnRequest.getCode()))
                .filter(Objects::nonNull)
                .toList();

        // Build data items for each report model
        for (ReportModel reportModel : reportModels) {
            ReportSummaryResponse.DataItem dataItem = new ReportSummaryResponse.DataItem();

            // Convert date from yyyy-MM-dd to dd/MM/yyyy
            dataItem.setDate(reportModel.getReportDate());

            // Build extraData based on requested columns
            Map<String, Object> extraData = new HashMap<>();
            for (ColumnConfig columnConfig : columnConfigs) {
                String code = columnConfig.getCode();
                String mapping = columnConfig.getMapping();

                Object value = getValueFromReportModel(reportModel, mapping);
                extraData.put(code, value);
            }

            dataItem.setExtraData(extraData);
            dataItems.add(dataItem);
        }

        response.setData(dataItems);

        // Build summary - sum/average of all columns
        Map<String, Object> summary = buildSummary(reportModels, columnConfigs);
        response.setSummary(summary);

        return response;
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
            if (columnConfig.getSummaryType() == null || columnConfig.getSummaryType().toString().equals("SUM")) {
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
                yield sum.divide(BigDecimal.valueOf(reportModels.size()), 2, RoundingMode.HALF_UP);
            }
            case "ads_cost_per_confirmed_order" -> {
                BigDecimal sum = reportModels.stream()
                        .map(ReportModel::getAdCostPerConfirmedOrder)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(reportModels.size()), 2, RoundingMode.HALF_UP);
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
