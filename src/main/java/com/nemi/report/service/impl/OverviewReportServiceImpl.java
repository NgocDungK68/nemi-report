package com.nemi.report.service.impl;

import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.model.request.OverviewReportRequest;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.response.ConfigResponse;
import com.nemi.report.model.response.OverviewReportResponse;
import com.nemi.report.model.response.RevenueSummary;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.OverviewReportService;
import com.nemi.report.util.ReportUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private int percentScale;

    @PostConstruct
    public void init(){
        percentScale = reportConfig.getScale().getPercent();
    }

    @Override
    public OverviewReportResponse getOverviewReport(OverviewReportRequest request) {
        log.info("[OverviewReportServiceImpl.getOverviewReport] Start calculating overview report: from={} to={} compareWith={} currency={}",
                request.getFrom(), request.getTo(), request.getCompareWith(), request.getCurrency());

        ConfigResponse config = configService.getConfig();

        List<String> totalOrderStatus = OrderStatus.getTotalOrdersStatus();
        List<String> returnedOrdersStatus = config.getReturnOrderWhen().getOrderStatus();
        List<String> confirmedOrderStatus = config.getConfirmOrderWhen().getOrderStatus();
        List<String> deliveringOrderStatus = OrderStatus.getDeliveringOrdersStatus();

        LocalDateTime from = request.getFrom().atStartOfDay();
        LocalDateTime to = request.getTo().atTime(LocalTime.MAX);
        LocalDateTime compareFrom = from.minusDays(request.getCompareWith().getDays());
        LocalDateTime compareTo = to.minusDays(request.getCompareWith().getDays());
        ReportTimeRange currentPeriod = ReportTimeRange.of(from, to);
        ReportTimeRange previousPeriod = ReportTimeRange.of(compareFrom, compareTo);

        try {
            OverviewReportResponse.OrderData totalRevenueData = getOrderData(currentPeriod, previousPeriod, totalOrderStatus);
            OverviewReportResponse.OrderData returnedOrdersData = getOrderData(currentPeriod, previousPeriod, returnedOrdersStatus);
            OverviewReportResponse.OrderData confirmedOrdersData = getOrderData(currentPeriod, previousPeriod, confirmedOrderStatus);
            OverviewReportResponse.OrderData deliveringOrdersData = getOrderData(currentPeriod, previousPeriod, deliveringOrderStatus);
            OverviewReportResponse.CostData adCostData = getAdCostData(currentPeriod, previousPeriod);
            OverviewReportResponse.ProfitData profitData = getProfitData(currentPeriod, previousPeriod);

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
            throw new RuntimeException(e);
        }
    }

    private OverviewReportResponse.OrderData getOrderData(ReportTimeRange currentPeriod,
                                                          ReportTimeRange previousPeriod,
                                                          List<String> orderStatus) {
        log.debug("[OverviewReportServiceImpl.getOrderData] Getting orders for statuses: {} | Current: {} to {} | Previous: {} to {}",
                orderStatus, currentPeriod.getFrom(), currentPeriod.getTo(), previousPeriod.getFrom(), previousPeriod.getTo());

        RevenueSummary currentOrders = getOrderSummary(currentPeriod, orderStatus);
        RevenueSummary previousOrders = getOrderSummary(previousPeriod, orderStatus);

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

    /**
     * Hàm trả về doanh thu và số lượng orders
     */
    public RevenueSummary getOrderSummary(ReportTimeRange period, List<String> orderStatus) {
        log.debug("[OverviewReportServiceImpl.getOrderSummary] Querying orders with statuses={} in range {} to {}",
                orderStatus, period.getFrom(), period.getTo());

        LocalDateTime from = period.getFrom();
        LocalDateTime to = period.getTo();
        List<OrderEntity> orders = orderRepository.findByStatusInAndUpdatedAtBetween(orderStatus, from, to);
        log.debug("[OverviewReportServiceImpl.getOrderSummary] Found {} orders", orders.size());

        return RevenueSummary.builder()
                .revenue(getOrderRevenue(orders))
                .number(BigDecimal.valueOf(orders.size()))
                .build();
    }

    public BigDecimal getProfit(ReportTimeRange period) {
        log.debug("[OverviewReportServiceImpl.getProfit] Calculating profit for period {} to {}", period.getFrom(), period.getTo());

        RevenueSummary revenueSummary = getOrderSummary(period, OrderStatus.getTotalOrdersStatus());
        RevenueSummary returnedOrdersSummary = getOrderSummary(period, OrderStatus.getReturnedOrdersStatus());
        RevenueSummary adsSummary = getAdsSummary(period);

        BigDecimal profit = revenueSummary.getRevenue()
                .subtract(adsSummary.getRevenue().add(returnedOrdersSummary.getRevenue()));

        log.debug("[OverviewReportServiceImpl.getProfit] Profit computed: {}", profit);
        return profit;
    }

    /**
     * Lấy doanh thu và số lượng ads (chưa xử lý)
     */
    public RevenueSummary getAdsSummary(ReportTimeRange period) {
        LocalDateTime from = period.getFrom();
        LocalDateTime to = period.getTo();

        // TODO: code adCost
        return RevenueSummary.builder()
                .revenue(BigDecimal.ZERO)
                .number(BigDecimal.ZERO)
                .build();
    }

    private OverviewReportResponse.ProfitData getProfitData(ReportTimeRange currentPeriod,
                                                            ReportTimeRange previousPeriod) {
        // Lợi nhuận hiện tại & trước đó
        BigDecimal currentProfit = getProfit(currentPeriod);
        BigDecimal previousProfit = getProfit(previousPeriod);

        // % thay đổi lợi nhuận
        BigDecimal profitChangePercent = ReportUtils.changePercent(currentProfit, previousProfit, percentScale);

        return OverviewReportResponse.ProfitData.builder()
                .value(currentProfit)
                .changePercent(profitChangePercent)
                .build();
    }

    private OverviewReportResponse.CostData getAdCostData(ReportTimeRange currentPeriod,
                                                          ReportTimeRange previousPeriod) {
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
}
