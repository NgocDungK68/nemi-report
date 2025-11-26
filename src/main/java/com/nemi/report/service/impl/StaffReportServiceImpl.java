package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.configuration.StaffColumnConfig;
import com.nemi.report.constant.Limit;
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
import com.nemi.report.model.pojo.UserData;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.model.request.ReportSummaryRequest;
import com.nemi.report.model.request.staff.StaffChartRequest;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.model.response.staff.StaffChartResponse;
import com.nemi.report.model.response.staff.StaffDailyResponse;
import com.nemi.report.model.response.staff.StaffSummaryResponse;
import com.nemi.report.model.response.staff.StaffsChartResponse;
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
    public StaffDailyResponse getStaffDaily(String userId, ReportSummaryRequest request) {
        try {
            // 1. Get columns
            List<ColumnRequest> validColumns = ReportValidator.getValidColumn(request.getColumns(), staffColumnConfig.getListColumns());
            ReportSettingResponse reportSetting = configService.getConfig();

            // 2. Get list users with orders
            List<OrderQueryByDateModel> orderQueryModel = fetchOrderQueryByUserDateModels(
                    userId,
                    request.getCurrency(),
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting
            );

            // 3. Get ads cost if needed
            List<AdCostByDate> adCostByDates = new ArrayList<>();
            if (containsSource(request.getColumns(), ProductSource.ADS)) {
                adCostByDates = getAdsCostByUserDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), userId);
            }

            // 4. Combine data
            List<ReportByDateModel> reportModels = combineDataReportUserDate(orderQueryModel, adCostByDates, request.getStartDate(), request.getEndDate());

            // 5. Pagination
            List<ReportByDateModel> pagedModels = reportHelperService.paginateReportModels(reportModels, request.getPage(), request.getSize());

            // 6. Build response
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

    @Override
    public StaffsChartResponse getAllStaffsChart(StaffChartRequest request) {
        try {
            // 1. Get chart column
            ColumnConfig columnChart = staffColumnConfig.getColumnByCode(request.getChartData().getValue());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Get exchange rate
            BigDecimal lastExchangeRate = exchangeRateService.getLastExchangeRate(claimUtil.getCompanyId(), CurrencyCodeEnum.VND.name(), request.getCurrency().name(), request.getStartDate(), request.getEndDate());

            // 4. Inquiry user with order
            List<OrderQueryByUserModel> orderQueryModels = orderCustomRepository.reportOrderByUser(claimUtil.getDepartmentId(),
                    request.getStartDate(), request.getEndDate(),
                    List.of(), List.of(),
                    reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

            // 5. Exchange rate
            applyExchangeRatesToOrders(orderQueryModels, lastExchangeRate, request.getCurrency());

            // 6. Get ads cost if needed
            List<AdsCostOfUser> adsCostOfUsers = new ArrayList<>();
            if (containsSource(List.of(columnChart), ProductSource.ADS)) {
                List<String> userIds = orderQueryModels.stream().map(OrderQueryByUserModel::getUserId).toList();
                adsCostOfUsers = getAdsCostByUser(request.getCurrency(), request.getStartDate(), request.getEndDate(), userIds);
            }

            // 7. Combine order and ads cost
            List<ReportByUserModel> allUsers = combineDataReportUsers(orderQueryModels, adsCostOfUsers);

            // 8. Order by columnChart DESC and limit top
            List<ReportByUserModel> topUsers = sortAndGetTop(allUsers, columnChart.getCode(), request.getLimit());

            // 9. Calculate percent and map to response
            return buildStaffsChartResponse(topUsers, allUsers, columnChart, request);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getStaffsChart] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    @Override
    public StaffChartResponse getStaffChart(String userId, StaffChartRequest request) {
        try {
            // 1. Get chart column
            ColumnConfig columnChart = staffColumnConfig.getColumnByCode(request.getChartData().getValue());

            // 2. Get report config
            ReportSettingResponse reportSetting = configService.getConfig();

            // 3. Fetch order data by date for this user
            List<OrderQueryByDateModel> orderQueryByDate = orderCustomRepository.reportOrderByDateOfUser(
                    claimUtil.getDepartmentId(),
                    userId,
                    request.getStartDate(),
                    request.getEndDate(),
                    reportSetting.getConfirmOrderWhen(),
                    reportSetting.getReturnOrderWhen()
            );

            // 4. Exchange revenue
            reportHelperService.exchangeRevenue(orderQueryByDate, request.getCurrency(), request.getStartDate(), request.getEndDate());

            // 5. Get ads cost by date if needed
            List<AdCostByDate> adCostByDates = new ArrayList<>();
            if (ProductSource.ADS.equals(columnChart.getSource())) {
                adCostByDates = getAdsCostByUserDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), userId);
            }

            // 6. Combine data into date models
            List<ReportByDateModel> reportByDateModels = combineDataReportUserDate(
                    orderQueryByDate,
                    adCostByDates,
                    request.getStartDate(),
                    request.getEndDate()
            );

            // 7. Build per-date data list and summary
            return buildSingleStaffChartResponse(reportByDateModels, columnChart, request.getLimit());
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[getStaffChartResponse] error: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }
    }

    private StaffChartResponse buildSingleStaffChartResponse(List<ReportByDateModel> reportByDateModels, ColumnConfig columnChart, Limit limit) {
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
        List<StaffChartResponse.DateData> dateData = new ArrayList<>();

        // Add top dates
        for (ReportByDateModel model : topDates) {
            BigDecimal value = convertToBigDecimal(reportHelperService.getValueFromReportModel(model, columnChart.getCode()));
            BigDecimal percent = calculatePercent(value, totalValue);

            StaffChartResponse.DateData item = new StaffChartResponse.DateData();
            item.setDate(LocalDate.parse(model.getReportDate(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            item.setValue(value);
            item.setPercent(percent);
            dateData.add(item);
        }

        // Add "Other" date if top dates total is less than overall total
        if (topTotalValue.compareTo(totalValue) < 0) {
            BigDecimal otherValue = totalValue.subtract(topTotalValue);
            BigDecimal otherPercent = calculatePercent(otherValue, totalValue);

            StaffChartResponse.DateData otherItem = new StaffChartResponse.DateData();
            otherItem.setDate(null); // date = null for "Other"
            otherItem.setValue(otherValue);
            otherItem.setPercent(otherPercent);
            dateData.add(otherItem);
        }

        // Summary
        StaffChartResponse.Summary summary = new StaffChartResponse.Summary();
        summary.setValue(totalValue);
        summary.setPercent(BigDecimal.valueOf(100.0));

        StaffChartResponse response = new StaffChartResponse();
        response.setTotalElements(dateData.size());
        response.setDateData(dateData);
        response.setSummary(summary);
        return response;
    }

    // >>>>>>>>>>>>>>> PRIVATE HELPER METHODS >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

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
            UserData userData = new UserData();
            userData.setId(reportModel.getUserId());
            userData.setName(reportModel.getName());
            userData.setImage(reportModel.getImage());
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

    private List<ReportByUserModel> sortAndGetTop(List<ReportByUserModel> reportModels, String code, Limit limit) {
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

    private StaffsChartResponse buildStaffsChartResponse(List<ReportByUserModel> topUsers, List<ReportByUserModel> allUsers, ColumnConfig columnChart, StaffChartRequest request) {
        // Calculate total value from ALL users for percentage calculation
        BigDecimal totalValue = calculateTotalValue(allUsers, columnChart.getMapping());

        // Calculate total value from top users
        BigDecimal topTotalValue = calculateTotalValue(topUsers, columnChart.getMapping());

        List<StaffsChartResponse.StaffChartData> chartDataList = new ArrayList<>();

        // Add top users
        for (ReportByUserModel userModel : topUsers) {
            StaffsChartResponse.StaffChartData chartData = new StaffsChartResponse.StaffChartData();

            // Set user info
            UserData userData = new UserData();
            userData.setId(userModel.getUserId());
            userData.setName(userModel.getName());
            userData.setImage(userModel.getImage());
            chartData.setUser(userData);

            // Set data value
            Object valueObj = reportHelperService.getValueFromReportModel(userModel, columnChart.getCode());
            BigDecimal value = convertToBigDecimal(valueObj);
            BigDecimal percent = calculatePercent(value, totalValue);

            StaffsChartResponse.DataValue dataValue = new StaffsChartResponse.DataValue();
            dataValue.setValue(value);
            dataValue.setPercent(percent);
            chartData.setData(dataValue);

            // Set date values if splitByDate is true
            if (request.isSplitByDate()) {
                List<StaffsChartResponse.DateValue> dateValues = fetchDateValuesForUser(
                        userModel.getUserId(),
                        request,
                        columnChart
                );
                chartData.setDateValues(dateValues);
            }

            chartDataList.add(chartData);
        }

        // Add "Other" user if top total is less than overall total
        if (topTotalValue.compareTo(totalValue) < 0) {
            BigDecimal otherValue = totalValue.subtract(topTotalValue);
            BigDecimal otherPercent = calculatePercent(otherValue, totalValue);

            StaffsChartResponse.StaffChartData otherChartData = new StaffsChartResponse.StaffChartData();

            // Set "Other" user info
            UserData otherUserData = new UserData();
            otherUserData.setId(null);
            otherUserData.setName("Other");
            otherUserData.setImage(null);
            otherChartData.setUser(otherUserData);

            // Set data value for "Other"
            StaffsChartResponse.DataValue otherDataValue = new StaffsChartResponse.DataValue();
            otherDataValue.setValue(otherValue);
            otherDataValue.setPercent(otherPercent);
            otherChartData.setData(otherDataValue);

            // No date values for "Other" user
            otherChartData.setDateValues(null);

            chartDataList.add(otherChartData);
        }

        // Build summary
        StaffsChartResponse.Summary summary = new StaffsChartResponse.Summary();
        summary.setValue(totalValue);
        summary.setPercent(BigDecimal.valueOf(100.0));

        return StaffsChartResponse.builder()
                .totalElements(chartDataList.size())
                .userData(chartDataList)
                .summary(summary)
                .build();
    }

    private BigDecimal calculateTotalValue(List<ReportByUserModel> reportModels, String mapping) {
        return reportModels.stream()
                .map(model -> {
                    Object value = reportHelperService.getValueFromReportModel(model, mapping);
                    return convertToBigDecimal(value);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        } else if (value instanceof Long l) {
            return BigDecimal.valueOf(l);
        } else if (value instanceof Double d) {
            return BigDecimal.valueOf(d);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePercent(BigDecimal value, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<StaffsChartResponse.DateValue> fetchDateValuesForUser(String userId, StaffChartRequest request, ColumnConfig columnChart) {
        try {
            ReportSettingResponse reportSetting = configService.getConfig();

            // Fetch order data by date for this user
            List<OrderQueryByDateModel> orderQueryByDate = orderCustomRepository.reportOrderByDateOfUser(
                    claimUtil.getDepartmentId(),
                    userId,
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
                adCostByDates = getAdsCostByUserDate(request.getCurrency(), request.getStartDate(), request.getEndDate(), userId);
            }

            // Combine data
            List<ReportByDateModel> reportByDateModels = combineDataReportUserDate(
                    orderQueryByDate,
                    adCostByDates,
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
            List<StaffsChartResponse.DateValue> dateValues = new ArrayList<>();
            for (ReportByDateModel reportModel : reportByDateModels) {
                StaffsChartResponse.DateValue dateValue = new StaffsChartResponse.DateValue();
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
            log.error("[fetchDateValuesForUser] error for userId: {}, error: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
