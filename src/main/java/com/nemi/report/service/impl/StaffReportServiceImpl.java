package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.configuration.StaffColumnConfig;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.ads_manager.AdCostByDate;
import com.nemi.report.model.ads_manager.AdsCostOfUser;
import com.nemi.report.model.ads_manager.AdsCostOfUsersRequest;
import com.nemi.report.model.ads_manager.AdsCostOfUsersResponse;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByUserModel;
import com.nemi.report.model.pojo.ReportByDateModel;
import com.nemi.report.model.pojo.ReportByUserModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.model.response.staff.StaffDailyResponse;
import com.nemi.report.model.response.staff.StaffSummaryResponse;
import com.nemi.report.repository.OrderCustomRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.ExchangeRateService;
import com.nemi.report.service.StaffReportService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ReportValidator;
import com.nemi.util.ClaimUtil;
import com.nemi.util.CurrencyUtils;
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
public class StaffReportServiceImpl implements StaffReportService {

    private final ExchangeRateService exchangeRateService;
    private final ClaimUtil claimUtil;
    private final StaffColumnConfig staffColumnConfig;
    private final ConfigService configService;
    private final ReportHelperService reportHelperService;

    private final OrderCustomRepository orderCustomRepository;
    private final AdsManagerClient adsManagerClient;

    @Override
    public StaffSummaryResponse getStaffReportSummary(ReportSummaryRequest request) {
        try {
            // 1. filter request
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), staffColumnConfig.getListColumns());
            List<ColumnRequest> validOrders = ReportValidator.getValidOrders(request.getColumns(), staffColumnConfig.getListColumns());
            List<FilterRequest> validFilters = ReportValidator.getValidFilters(request.getFilters(), staffColumnConfig.getListColumns());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Get exchange rate
            BigDecimal lastExchangeRate = exchangeRateService.getLastExchangeRate(claimUtil.getCompanyId(), CurrencyCodeEnum.VND.name(), request.getCurrency().name(), request.getStartDate(), request.getEndDate());

            // 4. Inquiry user with order
            List<OrderQueryByUserModel> orderQueryModels = orderCustomRepository.reportOrderByUser(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    validOrders, validFilters,
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Exchange rate
            applyExchangeRatesToOrders(orderQueryModels, lastExchangeRate, request.getCurrency());

            // 6. Get ads cost if needed
            List<AdsCostOfUser> adsCostOfUsers = new ArrayList<>();
            if (containsSource(validColumns, ProductSource.ADS)) {
                List<String> userIds = orderQueryModels.stream().map(OrderQueryByUserModel::getUserId).toList();
                adsCostOfUsers = getAdsCostByUser(request.getCurrency(), request.getStartDate(), request.getEndDate(), userIds);
            }

            // 7. Combine order and ads cost
            List<ReportByUserModel> reportModels = combineDataReportUsers(orderQueryModels, adsCostOfUsers);

            // 8. Pagination
            List<ReportByUserModel> pagedModels = reportHelperService.paginateReportModels(reportModels, request.getPage(), request.getSize());

            // 9. Map to response
            StaffSummaryResponse response = buildStaffSummaryResponse(pagedModels, reportModels, validColumns);
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
    public StaffDailyResponse getStaffDailyResponse(String userId, ReportSummaryRequest request) {
        try {
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), staffColumnConfig.getListColumns());
            ReportSettingResponse reportSetting = configService.getConfig();

            List<OrderQueryByDateModel> orderQueryModel = fetchOrderQueryByUserDateModels(
                    userId,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting
            );

            // 6. Get ads cost if needed
            List<AdCostByDate> adCostByDates = new ArrayList<>();
            if (containsSource(request.getColumns(), ProductSource.ADS)) {
                adCostByDates = getAdsCostByUserDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), userId);
            }

            List<ReportByDateModel> reportModels = combineDataReportUserDate(orderQueryModel, adCostByDates, request.getStartDate(), request.getEndDate());

            // Pagination
            List<ReportByDateModel> pagedModels = reportHelperService.paginateReportModels(reportModels, request.getPage(), request.getSize());

            // Build response
            StaffDailyResponse response = buildReportStaffDailyResponse(pagedModels, reportModels, validColumns);
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

    private void applyExchangeRatesToOrders(List<OrderQueryByUserModel> orderQueryModel, BigDecimal rate, CurrencyCodeEnum currency) {
        orderQueryModel.forEach(order -> {
            if (order.getRevenue() != null) {
                order.setRevenue(CurrencyUtils.exchange(currency, order.getRevenue(), rate));
            }
            if (order.getTrueRevenue() != null) {
                order.setTrueRevenue(CurrencyUtils.exchange(currency, order.getTrueRevenue(), rate));
            }
        });
    }

    private boolean containsSource(List<?> columns, ProductSource source) {
        return columns.stream()
                .anyMatch(c -> {
                    ColumnConfig column = (c instanceof ColumnConfig)
                            ? (ColumnConfig) c
                            : staffColumnConfig.getColumnByCode(((ColumnRequest) c).getCode());
                    return Objects.nonNull(column) && source.equals(column.getSource());
                });
    }

    private List<AdsCostOfUser> getAdsCostByUser(CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, List<String> userIds) {
        AdsCostOfUsersRequest request = AdsCostOfUsersRequest.builder()
                .companyId(claimUtil.getCompanyId())
                .departmentId(claimUtil.getDepartmentId())
                .startDate(startDate)
                .endDate(endDate)
                .currency(currency.name())
                .userIds(userIds)
                .build();
        AdsCostOfUsersResponse response = adsManagerClient.getAdsCostOfUsers(request).getBody();
        if (Objects.isNull(response)) {
            log.error("[getAdsCostByUser] response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
        return response.getAdsCostOfUsers();
    }

    private List<AdCostByDate> getAdsCostByUserDate(CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, String userId) {
        AdsCostOfUsersRequest request = AdsCostOfUsersRequest.builder()
                .companyId(claimUtil.getCompanyId())
                .departmentId(claimUtil.getDepartmentId())
                .startDate(startDate)
                .endDate(endDate)
                .currency(currency.name())
                .userIds(List.of(userId))
                .build();
        AdsCostOfUsersResponse response = adsManagerClient.getAdsCostOfUsers(request).getBody();
        if (Objects.isNull(response) || response.getAdsCostOfUsers().isEmpty()) {
            log.error("[getAdsCostByUserDate] response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
        return response.getAdsCostOfUsers().get(0).getAdCostByDate();
    }

    private List<ReportByUserModel> combineDataReportUsers(List<OrderQueryByUserModel> orderQueryModels, List<AdsCostOfUser> adsCostOfUsers) {
        Map<String, BigDecimal> adCostMap = buildAdCostMap(adsCostOfUsers);

        List<ReportByUserModel> reportModels = new ArrayList<>();

        // Combine all users from both order and ads data
        for (OrderQueryByUserModel orderQuery : orderQueryModels) {
            String userId = orderQuery.getUserId();
            BigDecimal adCost = adCostMap.getOrDefault(userId, BigDecimal.ZERO);
            ReportByUserModel reportModel = buildReportModelForUser(orderQuery, adCost);
            reportModels.add(reportModel);
        }

        return reportModels;
    }

    private List<ReportByDateModel> combineDataReportUserDate(List<OrderQueryByDateModel> orderQueryModels, List<AdCostByDate> adCostByDates, LocalDate startDate, LocalDate endDate) {
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

    private StaffDailyResponse buildReportStaffDailyResponse(List<ReportByDateModel> pagedReportModels, List<ReportByDateModel> allReportModels, List<ColumnRequest> validColumns) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(validColumns);
        List<StaffDailyResponse.DataItem> dataItems = buildStaffDailyDataItems(pagedReportModels, columnConfigs);
        Map<String, Object> summary = reportHelperService.buildSummary(allReportModels, columnConfigs);

        StaffDailyResponse response = new StaffDailyResponse();
        response.setData(dataItems);
        response.setSummary(summary);

        return response;
    }

    private Map<String, BigDecimal> buildAdCostMap(List<AdsCostOfUser> adsCostOfUsers) {
        Map<String, BigDecimal> adCostMap = new HashMap<>();
        for (AdsCostOfUser adsCost : adsCostOfUsers) {
            adCostMap.put(adsCost.getUserId(), adsCost.getSpent());
        }
        return adCostMap;
    }

    private Map<String, OrderQueryByDateModel> buildOrderMap(List<OrderQueryByDateModel> orderQueryModels) {
        Map<String, OrderQueryByDateModel> orderMap = new HashMap<>();
        for (OrderQueryByDateModel orderQuery : orderQueryModels) {
            orderMap.put(orderQuery.getReportDate(), orderQuery);
        }
        return orderMap;
    }

    private Map<String, BigDecimal> buildAdCostMapByDate(List<AdCostByDate> adCostByDates) {
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
        calculateDerivedMetricsUserDate(reportModel, adCost);

        return reportModel;
    }

    private ReportByUserModel buildReportModelForUser(OrderQueryByUserModel orderQuery, BigDecimal adCost) {
        ReportByUserModel reportModel = new ReportByUserModel();

        // Copy user info
        reportModel.setUserId(orderQuery.getUserId());
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

    private void calculateDerivedMetrics(ReportByUserModel reportModel) {
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

    private StaffSummaryResponse buildStaffSummaryResponse(List<ReportByUserModel> pagedReportModels, List<ReportByUserModel> allReportModels, List<ColumnRequest> columns) {
        List<ColumnConfig> columnConfigs = getColumnConfigs(columns);
        List<StaffSummaryResponse.DataItem> dataItems = buildDataItems(pagedReportModels, columnConfigs);
        Map<String, Object> summary = reportHelperService.buildSummary(allReportModels, columnConfigs);

        StaffSummaryResponse response = new StaffSummaryResponse();
        response.setData(dataItems);
        response.setSummary(summary);

        return response;
    }

    private List<ColumnConfig> getColumnConfigs(List<ColumnRequest> columns) {
        return columns.stream()
                .map(columnRequest -> staffColumnConfig.getColumnByCode(columnRequest.getCode()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<StaffSummaryResponse.DataItem> buildDataItems(List<ReportByUserModel> reportModels, List<ColumnConfig> columnConfigs) {
        List<StaffSummaryResponse.DataItem> dataItems = new ArrayList<>();

        for (ReportByUserModel reportModel : reportModels) {
            StaffSummaryResponse.DataItem dataItem = new StaffSummaryResponse.DataItem();

            // Set user data
            StaffSummaryResponse.UserData userData = new StaffSummaryResponse.UserData();
            userData.setId(reportModel.getUserId());
            userData.setName(reportModel.getName());
            userData.setImageUrl(reportModel.getImage());
            dataItem.setUser(userData);

            // Set extra data
            dataItem.setExtraData(reportHelperService.buildExtraData(reportModel, columnConfigs));
            dataItems.add(dataItem);
        }

        return dataItems;
    }

    private List<StaffDailyResponse.DataItem> buildStaffDailyDataItems(List<ReportByDateModel> reportModels, List<ColumnConfig> columnConfigs) {
        List<StaffDailyResponse.DataItem> dataItems = new ArrayList<>();

        for (ReportByDateModel reportModel : reportModels) {
            StaffDailyResponse.DataItem dataItem = new StaffDailyResponse.DataItem();
            dataItem.setDate(reportModel.getReportDate());
            dataItem.setExtraData(reportHelperService.buildExtraData(reportModel, columnConfigs));
            dataItems.add(dataItem);
        }

        return dataItems;
    }

    private List<OrderQueryByDateModel> fetchOrderQueryByUserDateModels(String userId, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate, ReportSettingResponse reportSetting) {
        List<OrderQueryByDateModel> orderQueryModel;

        String departmentId = claimUtil.getDepartmentId();
        orderQueryModel = orderCustomRepository.reportOrderByDateOfUser(
                departmentId,
                userId,
                startDate,
                endDate,
                reportSetting.getConfirmOrderWhen(),
                reportSetting.getReturnOrderWhen()
        );

        reportHelperService.exchangeRevenue(orderQueryModel, currency, startDate, endDate);

        return orderQueryModel;
    }
}
