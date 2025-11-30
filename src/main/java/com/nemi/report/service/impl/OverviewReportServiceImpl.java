package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.OverviewChartType;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.ads_manager.AdCostByDate;
import com.nemi.report.model.ads_manager.AdsCostOfDepartmentRequest;
import com.nemi.report.model.ads_manager.AdsCostResponse;
import com.nemi.report.model.pojo.FullOrderQueryByDateModel;
import com.nemi.report.model.pojo.MonthlyReport;
import com.nemi.report.model.pojo.OrderQueryByHourModel;
import com.nemi.report.model.pojo.ReportByDateModel;
import com.nemi.report.model.request.overview.BusinessTodayRequest;
import com.nemi.report.model.request.overview.CompareChartRequest;
import com.nemi.report.model.request.overview.OverviewReportRequest;
import com.nemi.report.model.response.overview.BusinessTodayResponse;
import com.nemi.report.model.response.overview.CompareChartResponse;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.model.response.overview.OverviewReportResponse;
import com.nemi.report.repository.OrderCustomRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.OverviewReportService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ValidationUtils;
import com.nemi.util.ClaimUtil;
import com.nemi.util.DateUtils;
import jakarta.annotation.PostConstruct;
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
@RequiredArgsConstructor
@Slf4j
public class OverviewReportServiceImpl implements OverviewReportService {
    private final OrderCustomRepository orderCustomRepository;
    private final ConfigService configService;
    private final ReportConfig reportConfig;
    private final ClaimUtil claimUtil;
    private final AdsManagerClient adsManagerClient;
    private final ReportHelperService reportHelperService;

    private int percentScale;

    @PostConstruct
    public void init() {
        percentScale = reportConfig.getScale().getPercent();
    }

    @Override
    public OverviewReportResponse getOverviewReport(OverviewReportRequest request) {
        log.info("[OverviewReportServiceImpl.getOverviewReport] Start calculating overview report: from={} to={} compareWith={} currency={}",
                request.getFrom(), request.getTo(), request.getCompareWith(), request.getCurrency());

        try {
            ValidationUtils.validateTimeRange(request.getFrom(), request.getTo());
            CurrencyCodeEnum currency = request.getCurrency();

            // Get order status config
            ReportSettingResponse config = configService.getConfig();

            // Get date range
            LocalDate from = request.getFrom();
            LocalDate to = request.getTo();
            LocalDate compareFrom = from.minusDays(request.getCompareWith().getDays());
            LocalDate compareTo = to.minusDays(request.getCompareWith().getDays());

            // Get order report data using the completed getOrderReport method
            OverviewReportResponse orderReport = getOrderReport(currency, config, from, to, compareFrom, compareTo);

            // Calculate ad cost data
            OverviewReportResponse.CostData adCostData = calculateAdCostByDate(
                    currency,
                    orderReport.getTotalRevenue().getRevenue(),
                    orderReport.getTotalRevenue().getPreviousRevenue(),
                    from,
                    to,
                    compareFrom,
                    compareTo
            );
            orderReport.setAdCost(adCostData);

            // Calculate profit = true revenue - ad cost
            BigDecimal currentProfit = orderReport.getProfit().getValue().subtract(adCostData.getCost());
            BigDecimal previousProfit = orderReport.getProfit().getPreviousValue().subtract(adCostData.getPreviousCost());
            BigDecimal profitChangePercent = ReportUtils.changePercent(currentProfit, previousProfit, percentScale);

            // Update profit data with calculated values
            OverviewReportResponse.ProfitData finalProfitData = OverviewReportResponse.ProfitData.builder()
                    .value(currentProfit)
                    .previousValue(previousProfit)
                    .changePercent(profitChangePercent)
                    .build();
            orderReport.setProfit(finalProfitData);


            log.info("[OverviewReportServiceImpl.getOverviewReport] Successfully generated overview report for period {} to {}", request.getFrom(), request.getTo());

            return OverviewReportResponse.builder()
                    .totalRevenue(orderReport.getTotalRevenue())
                    .returnedOrders(orderReport.getReturnedOrders())
                    .confirmedOrders(orderReport.getConfirmedOrders())
                    .deliveringOrders(orderReport.getDeliveringOrders())
                    .adCost(adCostData)
                    .profit(finalProfitData)
                    .build();
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OverviewReportServiceImpl.getOverviewReport] Failed to get overview report: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.OVERVIEW_REPORT_ERROR));
        }
    }

    @Override
    public CompareChartResponse getCompareChart(CompareChartRequest request) {
        try {
            ValidationUtils.validateTimeRange(request.getFrom(), request.getTo());
            CurrencyCodeEnum currency = request.getCurrency();

            // Get order status config
            ReportSettingResponse reportSetting = configService.getConfig();

            // Get date range
            LocalDate startDate = request.getFrom();
            LocalDate endDate = request.getTo();
            LocalDate compareStartDate = startDate.minusDays(request.getCompareWith().getDays());
            LocalDate compareEndDate = endDate.minusDays(request.getCompareWith().getDays());

            // Get order report present and previous
            List<FullOrderQueryByDateModel> currentOrders = orderCustomRepository.fullReportOrderByDate(claimUtil.getDepartmentId(), startDate, endDate, reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());
            List<FullOrderQueryByDateModel> previousOrders = orderCustomRepository.fullReportOrderByDate(claimUtil.getDepartmentId(), compareStartDate, compareEndDate, reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());
            reportHelperService.exchangeRevenueForFullOrders(currentOrders, currency, startDate, endDate);
            reportHelperService.exchangeRevenueForFullOrders(previousOrders, currency, compareStartDate, compareEndDate);

            // Get ads cost if needed
            List<AdCostByDate> currentAdCosts = new ArrayList<>();
            List<AdCostByDate> previousAdCosts = new ArrayList<>();
            if (request.getDataType().isContainAdsCost()) {
                currentAdCosts = getAdsCostByDate(currency, startDate, endDate);
                previousAdCosts = getAdsCostByDate(currency, compareStartDate, compareEndDate);
            }

            List<ReportByDateModel> currentReport = combineData(currentOrders, currentAdCosts, startDate, endDate);
            List<ReportByDateModel> previousReport = combineData(previousOrders, previousAdCosts, compareStartDate, compareEndDate);

            // map to response
            List<CompareChartResponse.ChartDataPoint> dataPoints = new ArrayList<>();

            int size = currentReport.size();
            for (int i = 0; i < size; i++) {
                ReportByDateModel present = currentReport.get(i);
                ReportByDateModel previous = i < previousReport.size() ? previousReport.get(i) : null;

                BigDecimal presentValue = extractChartValue(present, request.getDataType());
                BigDecimal previousValue = previous != null ? extractChartValue(previous, request.getDataType()) : BigDecimal.ZERO;
                BigDecimal changePercent = ReportUtils.changePercent(presentValue, previousValue, percentScale);
                Long orders = extractChartOrders(present, request.getDataType());

                CompareChartResponse.ChartDataPoint point = CompareChartResponse.ChartDataPoint.builder()
                        .date(present.getReportDate())
                        .previousDate(previous != null ? previous.getReportDate() : null)
                        .presentValue(presentValue)
                        .previousValue(previousValue)
                        .changePercent(changePercent)
                        .orders(orders)
                        .build();
                dataPoints.add(point);
            }

            return CompareChartResponse.builder()
                    .data(dataPoints)
                    .build();

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OverviewReportServiceImpl.getCompareChart] Failed to get compare chart: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.OVERVIEW_REPORT_ERROR));
        }
    }

    @Override
    public BusinessTodayResponse getBusinessToday(BusinessTodayRequest request) {
        try {
            CurrencyCodeEnum currency = request.getCurrency();

            // Get order status config
            ReportSettingResponse reportSetting = configService.getConfig();

            // Get date range
            LocalDate startDate = DateUtils.vietnamToday();
            LocalDate endDate = DateUtils.vietnamToday();

            // get order report today
            List<FullOrderQueryByDateModel> reportByDates = orderCustomRepository.fullReportOrderByDate(
                    claimUtil.getDepartmentId(),
                    startDate,
                    endDate,
                    reportSetting.getConfirmOrderWhen(),
                    reportSetting.getReturnOrderWhen()
            );
            // exchange rate
            reportHelperService.exchangeRevenueForFullOrders(reportByDates, currency, startDate, endDate);

            FullOrderQueryByDateModel todayReport = !reportByDates.isEmpty() ? reportByDates.get(0) : new FullOrderQueryByDateModel();

            // get ads cost today
            BigDecimal todayAdCost = getAdsCostByDate(currency, startDate, endDate).stream()
                    .map(AdCostByDate::getSpent)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRevenue = nullToZero(todayReport.getRevenue());
            BigDecimal adCostPerRevenue = calculateAdCostPerRevenue(todayAdCost, totalRevenue);

            // Get true revenue by hour
            List<OrderQueryByHourModel> orderQueryByHourModels = orderCustomRepository.reportOrderByHour(claimUtil.getDepartmentId(), startDate, endDate, reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());
            reportHelperService.exchangeRevenueForTodayOrders(orderQueryByHourModels, currency);
            List<List<Integer>> hourFrames = reportConfig.getBusinessToday().getHourFrame();
            List<BusinessTodayResponse.HourFrameData> hourFrameData = new ArrayList<>();

            // Build hour frame data by aggregating hourly orders into configured frames
            Map<Integer, OrderQueryByHourModel> hourMap = new HashMap<>();
            for (OrderQueryByHourModel hourModel : orderQueryByHourModels) {
                hourMap.put(hourModel.getReportHour(), hourModel);
            }

            for (List<Integer> frame : hourFrames) {
                if (frame == null || frame.size() < 2) continue;
                int startHour = frame.get(0);
                int endHour = frame.get(1);

                BigDecimal frameRevenue = BigDecimal.ZERO;
                for (int hour = startHour; hour < endHour; hour++) {
                    OrderQueryByHourModel hourModel = hourMap.get(hour);
                    if (hourModel != null && hourModel.getTrueRevenue() != null) {
                        frameRevenue = frameRevenue.add(hourModel.getTrueRevenue());
                    }
                }

                hourFrameData.add(BusinessTodayResponse.HourFrameData.builder()
                        .hourFrame(startHour + "-" + endHour)
                        .value(frameRevenue)
                        .build());
            }

            // map to response
            return BusinessTodayResponse.builder()
                    .revenue(totalRevenue)
                    .adCost(todayAdCost)
                    .adCostPerRevenue(adCostPerRevenue)
                    .confirmedOrder(BusinessTodayResponse.OrderData.builder()
                            .revenue(nullToZero(todayReport.getConfirmedRevenue()))
                            .orders(todayReport.getConfirmedOrders())
                            .build())
                    .deliveredOrder(BusinessTodayResponse.OrderData.builder()
                            .revenue(nullToZero(todayReport.getDeliveringRevenue()))
                            .orders(todayReport.getDeliveringOrders())
                            .build())
                    .pendingOrder(BusinessTodayResponse.OrderData.builder()
                            .revenue(nullToZero(todayReport.getPendingRevenue()))
                            .orders(todayReport.getPendingOrders())
                            .build())
                    .canceledOrder(BusinessTodayResponse.OrderData.builder()
                            .revenue(nullToZero(todayReport.getCanceledRevenue()))
                            .orders(todayReport.getCanceledOrders())
                            .build())
                    .revenuePerHourFrame(hourFrameData)
                    .build();

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OverviewReportServiceImpl.getBusinessToday] Failed to get business today report: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.OVERVIEW_REPORT_ERROR));
        }
    }

    @Override
    public MonthlyReport getMonthlyReport(CurrencyCodeEnum currencyCode) {
        try {
            // get date of month
            LocalDate startDate = DateUtils.vietnamToday().withDayOfMonth(1);
            LocalDate endDate = DateUtils.vietnamToday();

            // Get order status config
            ReportSettingResponse config = configService.getConfig();

            // Get report by date
            List<FullOrderQueryByDateModel> orderQueryByDateModels = orderCustomRepository.fullReportOrderByDate(claimUtil.getDepartmentId(), startDate, endDate, config.getConfirmOrderWhen(), config.getReturnOrderWhen());
            reportHelperService.exchangeRevenueForFullOrders(orderQueryByDateModels, currencyCode, startDate, endDate);

            // Get ad cost of month
            BigDecimal monthlyAdCost = getAdsCostByDate(currencyCode, startDate, endDate).stream().map(AdCostByDate::getSpent).reduce(BigDecimal.ZERO, BigDecimal::add);

            // Aggregate
            BigDecimal totalRevenue = orderQueryByDateModels.stream()
                    .map(FullOrderQueryByDateModel::getRevenue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalOrders = orderQueryByDateModels.stream()
                    .mapToLong(FullOrderQueryByDateModel::getConfirmedOrders)
                    .sum();
            long totalReturnedOrders = orderQueryByDateModels.stream()
                    .mapToLong(FullOrderQueryByDateModel::getReturnedOrders)
                    .sum();

            // Today revenue
            String todayStr = DateUtils.dateToString(endDate);
            BigDecimal todayRevenue = orderQueryByDateModels.stream()
                    .filter(m -> todayStr.equals(m.getReportDate()))
                    .map(FullOrderQueryByDateModel::getRevenue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Ad cost per order
            BigDecimal adCostPerOrder = BigDecimal.ZERO;
            if (totalOrders > 0) {
                adCostPerOrder = monthlyAdCost.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
            }

            // Ad cost per revenue
            BigDecimal adCostPerRevenue = BigDecimal.ZERO;
            if (totalOrders > 0) {
                adCostPerRevenue = monthlyAdCost.divide(totalRevenue, 2, RoundingMode.HALF_UP);
            }

            MonthlyReport response = new MonthlyReport();
            response.setRevenue(totalRevenue);
            response.setTodayRevenue(todayRevenue);
            response.setAdCost(monthlyAdCost);
            response.setAdCostPerOrder(adCostPerOrder);
            response.setAdCostPerRevenue(adCostPerRevenue);
            response.setOrders(totalOrders);
            response.setReturnedOrders(totalReturnedOrders);

            return response;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OverviewReportServiceImpl.getMonthlyReport] Failed to get monthly report: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.OVERVIEW_REPORT_ERROR));
        }
    }


    // >>>>>>>>>>> private >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    private OverviewReportResponse getOrderReport(CurrencyCodeEnum currency, ReportSettingResponse reportSetting, LocalDate startDate, LocalDate endDate, LocalDate compareStartDate, LocalDate compareEndDate) {

        List<FullOrderQueryByDateModel> currentReport = orderCustomRepository.fullReportOrderByDate(claimUtil.getDepartmentId(), startDate, endDate, reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());
        List<FullOrderQueryByDateModel> previousReport = orderCustomRepository.fullReportOrderByDate(claimUtil.getDepartmentId(), compareStartDate, compareEndDate, reportSetting.getConfirmOrderWhen(), reportSetting.getReturnOrderWhen());

        reportHelperService.exchangeRevenueForFullOrders(currentReport, currency, startDate, endDate);
        reportHelperService.exchangeRevenueForFullOrders(previousReport, currency, compareStartDate, compareEndDate);

        // Aggregate current period data
        long currentTotalOrders = currentReport.stream().mapToLong(FullOrderQueryByDateModel::getOrders).sum();
        BigDecimal currentTotalRevenue = currentReport.stream().map(FullOrderQueryByDateModel::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long currentConfirmedOrders = currentReport.stream().mapToLong(FullOrderQueryByDateModel::getConfirmedOrders).sum();
        BigDecimal currentConfirmedRevenue = currentReport.stream().map(FullOrderQueryByDateModel::getConfirmedRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long currentReturnedOrders = currentReport.stream().mapToLong(FullOrderQueryByDateModel::getReturnedOrders).sum();
        BigDecimal currentReturnedRevenue = currentReport.stream().map(FullOrderQueryByDateModel::getReturnedRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long currentDeliveringOrders = currentReport.stream().mapToLong(FullOrderQueryByDateModel::getDeliveringOrders).sum();
        BigDecimal currentDeliveringRevenue = currentReport.stream().map(FullOrderQueryByDateModel::getDeliveringRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentTrueRevenue = currentReport.stream().map(FullOrderQueryByDateModel::getTrueRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Aggregate previous period data
        long previousTotalOrders = previousReport.stream().mapToLong(FullOrderQueryByDateModel::getOrders).sum();
        BigDecimal previousTotalRevenue = previousReport.stream().map(FullOrderQueryByDateModel::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long previousConfirmedOrders = previousReport.stream().mapToLong(FullOrderQueryByDateModel::getConfirmedOrders).sum();
        BigDecimal previousConfirmedRevenue = previousReport.stream().map(FullOrderQueryByDateModel::getConfirmedRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long previousReturnedOrders = previousReport.stream().mapToLong(FullOrderQueryByDateModel::getReturnedOrders).sum();
        BigDecimal previousReturnedRevenue = previousReport.stream().map(FullOrderQueryByDateModel::getReturnedRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        long previousDeliveringOrders = previousReport.stream().mapToLong(FullOrderQueryByDateModel::getDeliveringOrders).sum();
        BigDecimal previousDeliveringRevenue = previousReport.stream().map(FullOrderQueryByDateModel::getDeliveringRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousTrueRevenue = previousReport.stream().map(FullOrderQueryByDateModel::getTrueRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate change percentages
        BigDecimal totalRevenueChangePercent = ReportUtils.changePercent(currentTotalRevenue, previousTotalRevenue, percentScale);
        BigDecimal totalOrdersChangePercent = ReportUtils.changePercent(BigDecimal.valueOf(currentTotalOrders), BigDecimal.valueOf(previousTotalOrders), percentScale);
        BigDecimal confirmedRevenueChangePercent = ReportUtils.changePercent(currentConfirmedRevenue, previousConfirmedRevenue, percentScale);
        BigDecimal confirmedOrdersChangePercent = ReportUtils.changePercent(BigDecimal.valueOf(currentConfirmedOrders), BigDecimal.valueOf(previousConfirmedOrders), percentScale);
        BigDecimal returnedRevenueChangePercent = ReportUtils.changePercent(currentReturnedRevenue, previousReturnedRevenue, percentScale);
        BigDecimal returnedOrdersChangePercent = ReportUtils.changePercent(BigDecimal.valueOf(currentReturnedOrders), BigDecimal.valueOf(previousReturnedOrders), percentScale);
        BigDecimal deliveringRevenueChangePercent = ReportUtils.changePercent(currentDeliveringRevenue, previousDeliveringRevenue, percentScale);
        BigDecimal deliveringOrdersChangePercent = ReportUtils.changePercent(BigDecimal.valueOf(currentDeliveringOrders), BigDecimal.valueOf(previousDeliveringOrders), percentScale);

        // Build OrderData objects
        OverviewReportResponse.OrderData totalOrders = OverviewReportResponse.OrderData.builder()
                .revenue(currentTotalRevenue)
                .previousRevenue(previousTotalRevenue)
                .revenueChangePercent(totalRevenueChangePercent)
                .orders(currentTotalOrders)
                .previousOrders(previousTotalOrders)
                .ordersChangePercent(totalOrdersChangePercent)
                .build();

        OverviewReportResponse.OrderData confirmedOrders = OverviewReportResponse.OrderData.builder()
                .revenue(currentConfirmedRevenue)
                .previousRevenue(previousConfirmedRevenue)
                .revenueChangePercent(confirmedRevenueChangePercent)
                .orders(currentConfirmedOrders)
                .previousOrders(previousConfirmedOrders)
                .ordersChangePercent(confirmedOrdersChangePercent)
                .build();

        OverviewReportResponse.OrderData returnedOrders = OverviewReportResponse.OrderData.builder()
                .revenue(currentReturnedRevenue)
                .previousRevenue(previousReturnedRevenue)
                .revenueChangePercent(returnedRevenueChangePercent)
                .orders(currentReturnedOrders)
                .previousOrders(previousReturnedOrders)
                .ordersChangePercent(returnedOrdersChangePercent)
                .build();

        OverviewReportResponse.OrderData deliveringOrders = OverviewReportResponse.OrderData.builder()
                .revenue(currentDeliveringRevenue)
                .previousRevenue(previousDeliveringRevenue)
                .revenueChangePercent(deliveringRevenueChangePercent)
                .orders(currentDeliveringOrders)
                .previousOrders(previousDeliveringOrders)
                .ordersChangePercent(deliveringOrdersChangePercent)
                .build();

        BigDecimal trueRevenueChangePercent = ReportUtils.changePercent(currentTrueRevenue, previousTrueRevenue, percentScale);

        OverviewReportResponse.ProfitData profitData = OverviewReportResponse.ProfitData.builder()
                .value(currentTrueRevenue)
                .previousValue(previousTrueRevenue)
                .changePercent(trueRevenueChangePercent)
                .build();

        return OverviewReportResponse.builder()
                .totalRevenue(totalOrders)
                .confirmedOrders(confirmedOrders)
                .returnedOrders(returnedOrders)
                .deliveringOrders(deliveringOrders)
                .profit(profitData)
                .build();
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

    private OverviewReportResponse.CostData calculateAdCostByDate(CurrencyCodeEnum currency, BigDecimal currentRevenue, BigDecimal previousRevenue, LocalDate startDate, LocalDate endDate, LocalDate compareStartDate, LocalDate compareEndDate) {
        BigDecimal currentAdCost = getAdsCostByDate(currency, startDate, endDate).stream().map(AdCostByDate::getSpent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal previousAdCost = getAdsCostByDate(currency, compareStartDate, compareEndDate).stream().map(AdCostByDate::getSpent).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentage change in ad cost
        BigDecimal adCostChangePercent = ReportUtils.changePercent(currentAdCost, previousAdCost, percentScale);

        // Calculate ad cost per revenue
        BigDecimal currentAdCostPerRevenue = BigDecimal.valueOf(reportHelperService.calculateAdCostPerRevenue(currentAdCost, currentRevenue));
        BigDecimal previousAdCostPerRevenue = BigDecimal.valueOf(reportHelperService.calculateAdCostPerRevenue(previousAdCost, previousRevenue));
        BigDecimal adCostPerRevenueChangePercent = ReportUtils.changePercent(currentAdCostPerRevenue, previousAdCostPerRevenue, percentScale);

        return OverviewReportResponse.CostData.builder()
                .cost(currentAdCost)
                .previousCost(previousAdCost)
                .costChangePercent(adCostChangePercent)
                .adCostPerRevenue(currentAdCostPerRevenue)
                .previousAdCostPerRevenue(previousAdCostPerRevenue)
                .adCostPerRevenueChangePercent(adCostPerRevenueChangePercent)
                .build();
    }

    private List<ReportByDateModel> combineData(List<FullOrderQueryByDateModel> orderQueryModels, List<AdCostByDate> adCostByDates, LocalDate startDate, LocalDate endDate) {
        Map<String, FullOrderQueryByDateModel> orderMap = buildOrderMap(orderQueryModels);
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

    private Map<String, FullOrderQueryByDateModel> buildOrderMap(List<FullOrderQueryByDateModel> orderQueryModels) {
        Map<String, FullOrderQueryByDateModel> orderMap = new HashMap<>();
        for (FullOrderQueryByDateModel orderQuery : orderQueryModels) {
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

    private ReportByDateModel buildReportModelForDate(String dateStr, Map<String, FullOrderQueryByDateModel> orderMap, Map<String, BigDecimal> adCostMap) {
        FullOrderQueryByDateModel orderQuery = orderMap.get(dateStr);
        BigDecimal adCost = adCostMap.getOrDefault(dateStr, BigDecimal.ZERO);

        ReportByDateModel reportModel = new ReportByDateModel();
        reportModel.setReportDate(dateStr);

        reportHelperService.populateFullOrderDataByDate(reportModel, orderQuery);
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

    private BigDecimal extractChartValue(ReportByDateModel model, OverviewChartType type) {
        return switch (type) {
            case REVENUE -> nullToZero(model.getRevenue());
            case AD_COST -> nullToZero(model.getAdCost());
            case AD_COST_PER_REVENUE -> {
                // adCostPerRevenue is Double in model; convert safely
                Double acpr = model.getAdCostPerRevenue();
                yield BigDecimal.valueOf(acpr == null ? 0d : acpr);
            }
            case RETURNED_REVENUE -> nullToZero(model.getReturnedRevenue());
            case PROFIT -> nullToZero(model.getProfit());
        };
    }

    private Long extractChartOrders(ReportByDateModel model, OverviewChartType type) {
        return switch (type) {
            case REVENUE -> model.getConfirmedOrders();
            case AD_COST, AD_COST_PER_REVENUE, PROFIT -> null;
            case RETURNED_REVENUE -> model.getReturnedOrders();
        };
    }

    private BigDecimal nullToZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    private BigDecimal calculateAdCostPerRevenue(BigDecimal adCost, BigDecimal revenue) {
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return adCost == null ? BigDecimal.ZERO : adCost.divide(revenue, 4, java.math.RoundingMode.HALF_UP);
    }
}
