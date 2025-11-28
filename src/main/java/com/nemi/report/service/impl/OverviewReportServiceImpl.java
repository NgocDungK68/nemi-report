package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.AdsManagerClient;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.ads_manager.AdCostByDate;
import com.nemi.report.model.ads_manager.AdsCostOfDepartmentRequest;
import com.nemi.report.model.ads_manager.AdsCostResponse;
import com.nemi.report.model.pojo.FullOrderQueryByDateModel;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.overview.OverviewReportRequest;
import com.nemi.report.model.response.overview.ReportSettingResponse;
import com.nemi.report.model.response.overview.OverviewReportResponse;
import com.nemi.report.model.response.overview.RevenueSummary;
import com.nemi.report.repository.OrderCustomRepository;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.OverviewReportService;
import com.nemi.report.util.ReportUtils;
import com.nemi.report.util.ValidationUtils;
import com.nemi.util.ClaimUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverviewReportServiceImpl implements OverviewReportService {
    private final OrderRepository orderRepository;
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

    /**
     * Hàm trả về doanh thu và số lượng orders
     */
    @Override
    public RevenueSummary getOrderSummary(List<String> orderStatus, CurrencyRates currencyRates) {
        log.debug("[OverviewReportServiceImpl.getOrderSummary] Querying orders with statuses={} in range {} to {}",
                orderStatus, currencyRates.getFrom(), currencyRates.getTo());

        if (ObjectUtils.isEmpty(currencyRates.getCurrencyRate())) {
            List<OrderEntity> orders = orderRepository.findByDepartmentIdAndStatusInAndUpdatedAtBetween(claimUtil.getDepartmentId(), orderStatus, currencyRates.getFrom(), currencyRates.getTo());
            log.debug("[OverviewReportServiceImpl.getOrderSummary] Found {} orders", orders.size());

            return RevenueSummary.builder()
                    .revenue(getOrderRevenue(orders))
                    .number(BigDecimal.valueOf(orders.size()))
                    .build();
        }

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal numberOfOrders = BigDecimal.ZERO;
        for (LocalDate date = currencyRates.getFrom().toLocalDate();
             !date.isAfter(currencyRates.getTo().toLocalDate());
             date = date.plusDays(1)) {
            BigDecimal currencyRate = currencyRates.getCurrencyRate().get(date);
            if (ObjectUtils.isEmpty(currencyRate)) {
                log.warn("[OverviewReportServiceImpl.getOrderSummary] No currency rate found for date: {}", date);
                continue;
            }

            LocalDateTime startOfDate = date.atStartOfDay();
            LocalDateTime endOfDate = date.atTime(LocalTime.MAX);
            List<OrderEntity> orders = orderRepository.findByDepartmentIdAndStatusInAndUpdatedAtBetween(claimUtil.getDepartmentId(), orderStatus, startOfDate, endOfDate);
            BigDecimal orderRevenue = getOrderRevenue(orders).multiply(currencyRate);

            revenue = revenue.add(orderRevenue);
            numberOfOrders = numberOfOrders.add(BigDecimal.valueOf(orders.size()));
        }

        return RevenueSummary.builder()
                .revenue(revenue)
                .number(numberOfOrders)
                .build();
    }

    @Override
    public BigDecimal getOrderRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Lấy doanh thu và số lượng ads (chưa xử lý)
     */
    @Override
    public RevenueSummary getAdsSummary(CurrencyRates currencyRates) {
        // TODO: code adCost
        return RevenueSummary.builder()
                .revenue(BigDecimal.ZERO)
                .number(BigDecimal.ZERO)
                .build();
    }

    @Override
    public BigDecimal getProfit(CurrencyRates currencyRates) {
        log.debug("[OverviewReportServiceImpl.getProfit] Calculating profit for period {} to {}", currencyRates.getFrom(), currencyRates.getTo());

        ReportSettingResponse config = configService.getConfig();
        RevenueSummary revenueSummary = getOrderSummary(OrderStatus.getTotalOrdersStatus(), currencyRates);
        RevenueSummary returnedOrdersSummary = getOrderSummary(config.getReturnOrderWhen().getOrderStatus(), currencyRates);
        RevenueSummary adsSummary = getAdsSummary(currencyRates);

        BigDecimal profit = revenueSummary.getRevenue()
                .subtract(adsSummary.getRevenue().add(returnedOrdersSummary.getRevenue()));

        log.debug("[OverviewReportServiceImpl.getProfit] Profit computed: {}", profit);
        return profit;
    }

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

    private BigDecimal getAdsCostByDate(CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate) {
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
        return response.getAdCostByDate().stream()
                .map(AdCostByDate::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OverviewReportResponse.CostData calculateAdCostByDate(CurrencyCodeEnum currency, BigDecimal currentRevenue, BigDecimal previousRevenue, LocalDate startDate, LocalDate endDate, LocalDate compareStartDate, LocalDate compareEndDate) {
        BigDecimal currentAdCost = getAdsCostByDate(currency, startDate, endDate);
        BigDecimal previousAdCost = getAdsCostByDate(currency, compareStartDate, compareEndDate);

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
}
