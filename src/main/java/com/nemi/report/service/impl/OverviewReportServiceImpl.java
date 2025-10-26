package com.nemi.report.service.impl;

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
    private final ReportUtils reportUtils;
    private final ConfigService configService;

    @Override
    public OverviewReportResponse getOverviewReport(OverviewReportRequest request) {
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
        RevenueSummary order = getOrderSummary(currentPeriod, orderStatus);
        RevenueSummary orderBefore = getOrderSummary(previousPeriod, orderStatus);

        BigDecimal totalRevenue = order.getRevenue();
        BigDecimal totalRevenueBefore = orderBefore.getRevenue();
        BigDecimal totalOrders = order.getNumber();
        BigDecimal totalOrdersBefore = orderBefore.getNumber();
        BigDecimal revenueChangePercent = reportUtils.changePercent(totalRevenue, totalRevenueBefore);
        BigDecimal ordersChangePercent = reportUtils.changePercent(totalOrders, totalOrdersBefore);

        return OverviewReportResponse.OrderData.builder()
                .revenue(totalRevenue)
                .revenueChangePercent(revenueChangePercent)
                .orders(totalOrders)
                .ordersChangePercent(ordersChangePercent)
                .build();
    }

    /**
     * Hàm trả về doanh thu và số lượng orders
     */
    public RevenueSummary getOrderSummary(ReportTimeRange period, List<String> orderStatus) {
        LocalDateTime from = period.getFrom();
        LocalDateTime to = period.getTo();
        List<OrderEntity> orders = orderRepository.findByStatusInAndUpdatedAtBetween(orderStatus, from, to);

        return RevenueSummary.builder()
                .revenue(getOrderRevenue(orders))
                .number(BigDecimal.valueOf(orders.size()))
                .build();
    }

    public BigDecimal getProfit(ReportTimeRange period) {
        RevenueSummary revenueSummary = getOrderSummary(period, OrderStatus.getTotalOrdersStatus());
        RevenueSummary returnedOrdersSummary = getOrderSummary(period, OrderStatus.getReturnedOrdersStatus());
        RevenueSummary adsSummary = getAdsSummary(period);

        return revenueSummary.getNumber().subtract(
                adsSummary.getNumber().add(
                        returnedOrdersSummary.getNumber()
                )
        );
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
        BigDecimal profitValue = getProfit(currentPeriod);
        BigDecimal profitValueBefore = getProfit(previousPeriod);

        // % thay đổi lợi nhuận
        BigDecimal profitChangePercent = reportUtils.changePercent(profitValue, profitValueBefore);

        return OverviewReportResponse.ProfitData.builder()
                .value(profitValue)
                .changePercent(profitChangePercent)
                .build();
    }

    private OverviewReportResponse.CostData getAdCostData(ReportTimeRange currentPeriod,
                                                          ReportTimeRange previousPeriod) {
        // TODO: code CostData
        return null;
    }

    public BigDecimal getOrderRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
