package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.request.overview.OverviewReportRequest;
import com.nemi.report.model.response.overview.ConfigResponse;
import com.nemi.report.model.response.overview.OverviewReportResponse;
import com.nemi.report.model.response.overview.RevenueSummary;
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
    private final ConfigService configService;
    private final ReportConfig reportConfig;
    private final CurrencyRateService currencyRateService;
    private final ClaimUtil claimUtil;

    private int percentScale;
    private int usdScale;

    @PostConstruct
    public void init() {
        usdScale = reportConfig.getScale().getUsd();
        percentScale = reportConfig.getScale().getPercent();
    }

    @Override
    public OverviewReportResponse getOverviewReport(OverviewReportRequest request) {
        log.info("[OverviewReportServiceImpl.getOverviewReport] Start calculating overview report: from={} to={} compareWith={} currency={}",
                request.getFrom(), request.getTo(), request.getCompareWith(), request.getCurrency());

        ValidationUtils.validateTimeRange(request.getFrom(), request.getTo());
        ConfigResponse config = configService.getConfig();
        Currency currency = request.getCurrency();

        List<String> totalOrderStatus = OrderStatus.getTotalOrdersStatus();
        List<String> returnedOrdersStatus = config.getReturnOrderWhen().getOrderStatus();
        List<String> confirmedOrderStatus = config.getConfirmOrderWhen().getOrderStatus();
        List<String> deliveringOrderStatus = OrderStatus.getDeliveringOrdersStatus();

        LocalDate from = request.getFrom();
        LocalDate to = request.getTo();
        LocalDate compareFrom = from.minusDays(request.getCompareWith().getDays());
        LocalDate compareTo = to.minusDays(request.getCompareWith().getDays());
        CurrencyRates currentCurrencyRates = currencyRateService.getCurrencyRate(claimUtil.getCompanyId(), from, to, currency);
        CurrencyRates periodCurrencyRates = currencyRateService.getCurrencyRate(claimUtil.getCompanyId(), compareFrom, compareTo, currency);

        try {
            OverviewReportResponse.OrderData totalRevenueData = getOrderData(totalOrderStatus, currentCurrencyRates, periodCurrencyRates);
            OverviewReportResponse.OrderData returnedOrdersData = getOrderData(returnedOrdersStatus, currentCurrencyRates, periodCurrencyRates);
            OverviewReportResponse.OrderData confirmedOrdersData = getOrderData(confirmedOrderStatus, currentCurrencyRates, periodCurrencyRates);
            OverviewReportResponse.OrderData deliveringOrdersData = getOrderData(deliveringOrderStatus, currentCurrencyRates, periodCurrencyRates);
            OverviewReportResponse.CostData adCostData = getAdCostData(currentCurrencyRates, periodCurrencyRates);
            OverviewReportResponse.ProfitData profitData = getProfitData(currentCurrencyRates, periodCurrencyRates);

            log.info("[OverviewReportServiceImpl.getOverviewReport] Successfully generated overview report for period {} to {}", request.getFrom(), request.getTo());

            return OverviewReportResponse.builder()
                    .totalRevenue(totalRevenueData)
                    .returnedOrders(returnedOrdersData)
                    .confirmedOrders(confirmedOrdersData)
                    .deliveringOrders(deliveringOrdersData)
                    .adCost(adCostData)
                    .profit(profitData)
                    .build();
        } catch (Exception e) {
            log.error("[OverviewReportServiceImpl.getOverviewReport] Failed to get overview report: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.OVERVIEW_REPORT_ERROR));
        }
    }

    private OverviewReportResponse.OrderData getOrderData(List<String> orderStatus,
                                                          CurrencyRates currentCurrencyRates,
                                                          CurrencyRates periodCurrencyRates) {
        log.debug("[OverviewReportServiceImpl.getOrderData] Getting orders for statuses: {} | Current: {} to {} | Previous: {} to {}",
                orderStatus, currentCurrencyRates.getFrom(), currentCurrencyRates.getTo(), periodCurrencyRates.getFrom(), periodCurrencyRates.getTo());

        RevenueSummary currentOrders = getOrderSummary(orderStatus, currentCurrencyRates);
        RevenueSummary previousOrders = getOrderSummary(orderStatus, periodCurrencyRates);

        BigDecimal currentRevenue = currentOrders.getRevenue();
        BigDecimal previousRevenue = previousOrders.getRevenue();
        BigDecimal currentOrdersNumber = currentOrders.getNumber();
        BigDecimal previousOrdersNumber = previousOrders.getNumber();
        BigDecimal revenueChangePercent = ReportUtils.changePercent(currentRevenue, previousRevenue, percentScale);
        BigDecimal ordersChangePercent = ReportUtils.changePercent(currentOrdersNumber, previousOrdersNumber, percentScale);

        log.debug("[OverviewReportServiceImpl.getOrderData] Revenue: current={} previous={} change={}%; Orders: current={} previous={} change={}%",
                currentOrders.getRevenue(), previousOrders.getRevenue(), revenueChangePercent,
                currentOrders.getNumber(), previousOrders.getNumber(), ordersChangePercent);

        return OverviewReportResponse.OrderData.builder()
                .revenue(currentRevenue)
                .revenueChangePercent(revenueChangePercent)
                .orders(currentOrdersNumber)
                .ordersChangePercent(ordersChangePercent)
                .build();
    }

    public BigDecimal getProfit(CurrencyRates currencyRates) {
        ReportTimeRange timeRange = ReportTimeRange.of(currencyRates.getFrom(), currencyRates.getTo());
        log.debug("[OverviewReportServiceImpl.getProfit] Calculating profit for period {} to {}", timeRange.getFrom(), timeRange.getTo());

        RevenueSummary revenueSummary = getOrderSummary(OrderStatus.getTotalOrdersStatus(), currencyRates);
        RevenueSummary returnedOrdersSummary = getOrderSummary(OrderStatus.getReturnedOrdersStatus(), currencyRates);
        RevenueSummary adsSummary = getAdsSummary(currencyRates);

        BigDecimal profit = revenueSummary.getRevenue()
                .subtract(adsSummary.getRevenue().add(returnedOrdersSummary.getRevenue()));

        log.debug("[OverviewReportServiceImpl.getProfit] Profit computed: {}", profit);
        return profit;
    }

    /**
     * Lấy doanh thu và số lượng ads (chưa xử lý)
     */
    public RevenueSummary getAdsSummary(CurrencyRates currencyRates) {
        // TODO: code adCost
        return RevenueSummary.builder()
                .revenue(BigDecimal.ZERO)
                .number(BigDecimal.ZERO)
                .build();
    }

    private OverviewReportResponse.ProfitData getProfitData(CurrencyRates currentCurrencyRates,
                                                            CurrencyRates periodCurrencyRates) {
        // Lợi nhuận hiện tại & trước đó
        BigDecimal currentProfit = getProfit(currentCurrencyRates);
        BigDecimal previousProfit = getProfit(periodCurrencyRates);

        // % thay đổi lợi nhuận
        BigDecimal profitChangePercent = ReportUtils.changePercent(currentProfit, previousProfit, percentScale);

        return OverviewReportResponse.ProfitData.builder()
                .value(currentProfit)
                .changePercent(profitChangePercent)
                .build();
    }

    private OverviewReportResponse.CostData getAdCostData(CurrencyRates currentCurrencyRates,
                                                          CurrencyRates periodCurrencyRates) {
        // TODO: code CostData
        return OverviewReportResponse.CostData.builder()
                .cost(BigDecimal.ZERO)
                .costChangePercent(BigDecimal.ZERO)
                .adCostPerRevenue(BigDecimal.ZERO)
                .adCostPerRevenueChangePercent(BigDecimal.ZERO)
                .build();
    }

    public BigDecimal getOrderRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Hàm trả về doanh thu và số lượng orders
     */
    public RevenueSummary getOrderSummary(List<String> orderStatus, CurrencyRates currencyRates) {
        log.debug("[OverviewReportServiceImpl.getOrderSummary] Querying orders with statuses={} in range {} to {}",
                orderStatus, currencyRates.getFrom(), currencyRates.getTo());

        ReportTimeRange timeRange = ReportTimeRange.of(currencyRates.getFrom(), currencyRates.getTo());

        if (ObjectUtils.isEmpty(currencyRates.getCurrencyRate())) {
            List<OrderEntity> orders = orderRepository.findByStatusInAndUpdatedAtBetween(orderStatus, timeRange.getFrom(), timeRange.getTo());
            log.debug("[OverviewReportServiceImpl.getOrderSummary] Found {} orders", orders.size());

            return RevenueSummary.builder()
                    .revenue(getOrderRevenue(orders))
                    .number(BigDecimal.valueOf(orders.size()))
                    .build();
        }

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal numberOfOrders = BigDecimal.ZERO;
        for (LocalDate date = currencyRates.getFrom(); !date.isAfter(currencyRates.getTo()); date = date.plusDays(1)) {
            BigDecimal currencyRate = currencyRates.getCurrencyRate().get(date);
            if (ObjectUtils.isEmpty(currencyRate)) {
                log.warn("[OverviewReportServiceImpl.getOrderSummary] No currency rate found for date: {}", date);
                continue;
            }

            LocalDateTime startOfDate = date.atStartOfDay();
            LocalDateTime endOfDate = date.atTime(LocalTime.MAX);
            List<OrderEntity> orders = orderRepository.findByStatusInAndUpdatedAtBetween(orderStatus, startOfDate, endOfDate);
            BigDecimal orderRevenue = getOrderRevenue(orders).multiply(currencyRate);

            revenue = revenue.add(orderRevenue);
            numberOfOrders = numberOfOrders.add(BigDecimal.valueOf(orders.size()));
        }

        return RevenueSummary.builder()
                .revenue(revenue)
                .number(numberOfOrders)
                .build();
    }
}
