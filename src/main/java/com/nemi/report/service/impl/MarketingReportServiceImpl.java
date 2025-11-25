package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.configuration.MarketingColumnConfig;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.ads_manager.AdCostByDate;
import com.nemi.report.model.ads_manager.AdsCostOfDepartmentRequest;
import com.nemi.report.model.ads_manager.AdsCostResponse;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.ReportByDateModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.marketing.MarketingChartRequest;
import com.nemi.report.model.response.marketing.MarketingChartResponse;
import com.nemi.report.model.response.marketing.MarketingDataItem;
import com.nemi.report.model.response.marketing.MarketingReportResponse;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.repository.OrderCustomRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.MarketingReportService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ReportValidator;
import com.nemi.util.ClaimUtil;
import com.nemi.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final ReportHelperService reportHelperService;

    @Override
    public MarketingReportResponse getMarketingReportSummary(ReportSummaryRequest request) {
        try {
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), marketingColumnConfig.getListColumns());
            ReportSettingResponse reportSetting = configService.getConfig();

            List<OrderQueryByDateModel> orderQueryModel = fetchOrderQueryModels(
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

            List<ReportByDateModel> reportModels = combineData(orderQueryModel, adCostByDate, request.getStartDate(), request.getEndDate());

            // Pagination
            List<ReportByDateModel> pagedModels = reportHelperService.paginateReportModels(reportModels, request.getPage(), request.getSize());

            // Build response
            MarketingReportResponse response = buildReportSummaryResponse(pagedModels, reportModels, validColumns);
            response.setTotalElements((long) reportModels.size());
            response.setTotalPages(ReportUtils.calculateTotalPages(reportModels.size(), request.getSize()));
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

            List<OrderQueryByDateModel> orderQueryModel = fetchOrderQueryModels(
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

            List<ReportByDateModel> reportModels = combineData(orderQueryModel, adCostByDate, request.getStartDate(), request.getEndDate());

            return buildMarketingChartResponse(reportModels, columnChart);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getMarketingChart] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private List<OrderQueryByDateModel> fetchOrderQueryModels(Object requestOrColumn, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, ReportSettingResponse reportSetting) {
        List<OrderQueryByDateModel> orderQueryModel = new ArrayList<>();

        boolean needsOrderData = (requestOrColumn instanceof ReportSummaryRequest)
                ? isContainOrder((ReportSummaryRequest) requestOrColumn)
                : isContainOrder((ColumnConfig) requestOrColumn);

        if (needsOrderData) {
            String departmentId = claimUtil.getDepartmentId();
            orderQueryModel = orderCustomRepository.reportOrderByDate(
                    departmentId,
                    startDate,
                    endDate,
                    reportSetting.getConfirmOrderWhen(),
                    reportSetting.getReturnOrderWhen()
            );

            reportHelperService.exchangeRevenue(orderQueryModel, currency, startDate, endDate);
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
                            : marketingColumnConfig.getColumnByCode(((ColumnRequest) c).getCode());
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

    private List<ReportByDateModel> combineData(List<OrderQueryByDateModel> orderQueryModels, List<AdCostByDate> adCostByDates, LocalDate startDate, LocalDate endDate) {
        Map<String, OrderQueryByDateModel> orderMap = buildOrderMap(orderQueryModels);
        Map<String, BigDecimal> adCostMap = buildAdCostMap(adCostByDates);

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

    private Map<String, BigDecimal> buildAdCostMap(List<AdCostByDate> adCostByDates) {
        Map<String, BigDecimal> adCostMap = new HashMap<>();
        for (AdCostByDate adCost : adCostByDates) {
            adCostMap.put(adCost.getReportDate(), adCost.getSpent());
        }
        return adCostMap;
    }

    private ReportByDateModel buildReportModelForDate(String dateStr, Map<String, OrderQueryByDateModel> orderMap, Map<String, BigDecimal> adCostMap) {
        OrderQueryByDateModel orderQuery = orderMap.get(dateStr);
        BigDecimal adCost = adCostMap.getOrDefault(dateStr, BigDecimal.ZERO);

        ReportByDateModel reportModel = new ReportByDateModel();
        reportModel.setReportDate(dateStr);

        reportHelperService.populateOrderDataByDate(reportModel, orderQuery);
        reportModel.setAdCost(adCost);
        calculateDerivedMetrics(reportModel, adCost);

        return reportModel;
    }

    private void calculateDerivedMetrics(ReportByDateModel reportModel, BigDecimal adCost) {
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

    private MarketingReportResponse buildReportSummaryResponse(List<ReportByDateModel> pagedReportModels, List<ReportByDateModel> allReportModels, List<ColumnRequest> validColumns) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(validColumns);
        List<MarketingDataItem> dataItems = buildDataItems(pagedReportModels, columnConfigs);
        Map<String, Object> summary = reportHelperService.buildSummary(allReportModels, columnConfigs);

        MarketingReportResponse response = new MarketingReportResponse();
        response.setData(dataItems);
        response.setSummary(summary);

        return response;
    }

    private List<ColumnConfig> getColumnConfigs(List<ColumnRequest> columns) {
        return columns.stream()
                .map(columnRequest -> marketingColumnConfig.getColumnByCode(columnRequest.getCode()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<MarketingDataItem> buildDataItems(List<ReportByDateModel> reportModels, List<ColumnConfig> columnConfigs) {
        List<MarketingDataItem> dataItems = new ArrayList<>();

        for (ReportByDateModel reportModel : reportModels) {
            MarketingDataItem dataItem = new MarketingDataItem();
            dataItem.setDate(reportModel.getReportDate());
            dataItem.setExtraData(reportHelperService.buildExtraData(reportModel, columnConfigs));
            dataItems.add(dataItem);
        }

        return dataItems;
    }

    private MarketingChartResponse buildMarketingChartResponse(List<ReportByDateModel> reportModels, ColumnConfig columnConfig) {
        List<ColumnConfig> columnConfigs = List.of(columnConfig);
        List<MarketingDataItem> dataItems = buildDataItems(reportModels, columnConfigs);

        return MarketingChartResponse.builder()
                .data(dataItems)
                .build();
    }
}
