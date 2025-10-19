package com.nemi.report.service.impl;

import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.model.request.OverviewReportRequest;
import com.nemi.report.model.response.OverviewReportResponse;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.OverviewReportService;
import com.nemi.report.util.ReportUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OverviewReportServiceImpl implements OverviewReportService {
    private final OrderRepository orderRepository;
    private final ReportUtil reportUtil;

    @Override
    public OverviewReportResponse getOverviewReport(OverviewReportRequest request) {
        List<String> returnOrdersStatus = List.of(
                OrderStatus.RETURNED.getCode(),
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.FAILED.getCode()
        );

        List<String> confirmedOrderStatus = List.of(
                OrderStatus.DELIVERED.getCode(),
                OrderStatus.SHIPPING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode()
        );

        List<String> deliveringOrderStatus = List.of(
                OrderStatus.SHIPPING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode()
        );

        return OverviewReportResponse.builder()
                .totalRevenue(getRevenueData(request))
                .returnedOrders(getOrderData(request, returnOrdersStatus))
                .confirmedOrders(getOrderData(request, confirmedOrderStatus))
                .deliveringOrders(getOrderData(request, deliveringOrderStatus))
                .build();
    }

    private OverviewReportResponse.RevenueData getRevenueData(OverviewReportRequest request) {
        LocalDateTime from = request.getFrom().atStartOfDay();
        LocalDateTime to = request.getTo().atTime(LocalTime.MAX);
        LocalDateTime compareFrom = from.minusDays(request.getCompareWith().getDays());
        LocalDateTime compareTo = to.minusDays(request.getCompareWith().getDays());

        List<OrderEntity> orders = orderRepository.findByCreatedAtBetween(from, to);
        List<OrderEntity> ordersBefore = orderRepository.findByCreatedAtBetween(compareFrom, compareTo);

        BigDecimal totalRevenue = getRevenue(orders);
        BigDecimal totalRevenueBefore = getRevenue(ordersBefore);
        BigDecimal totalOrders = BigDecimal.valueOf(orders.size());
        BigDecimal totalOrdersBefore = BigDecimal.valueOf(ordersBefore.size());
        BigDecimal revenueChangePercent = reportUtil.changePercent(totalRevenue, totalRevenueBefore);
        BigDecimal ordersChangePercent = reportUtil.changePercent(totalOrders, totalOrdersBefore);

        return OverviewReportResponse.RevenueData.builder()
                .revenue(totalRevenue)
                .revenueChangePercent(revenueChangePercent)
                .orders(totalOrders)
                .ordersChangePercent(ordersChangePercent)
                .build();
    }

    private OverviewReportResponse.OrderData getOrderData(OverviewReportRequest request, List<String> orderStatus) {
        LocalDateTime from = request.getFrom().atStartOfDay();
        LocalDateTime to = request.getTo().atTime(LocalTime.MAX);
        LocalDateTime compareFrom = from.minusDays(request.getCompareWith().getDays());
        LocalDateTime compareTo = to.minusDays(request.getCompareWith().getDays());

        List<OrderEntity> orders = orderRepository.findByStatusInAndCreatedAtBetween(orderStatus, from, to);
        List<OrderEntity> ordersBefore = orderRepository.findByStatusInAndCreatedAtBetween(orderStatus, compareFrom, compareTo);

        BigDecimal totalRevenue = getRevenue(orders);
        BigDecimal totalRevenueBefore = getRevenue(ordersBefore);
        BigDecimal totalOrders = BigDecimal.valueOf(orders.size());
        BigDecimal totalOrdersBefore = BigDecimal.valueOf(ordersBefore.size());
        BigDecimal revenueChangePercent = reportUtil.changePercent(totalRevenue, totalRevenueBefore);
        BigDecimal ordersChangePercent = reportUtil.changePercent(totalOrders, totalOrdersBefore);

        return OverviewReportResponse.OrderData.builder()
                .revenue(getRevenue(orders))
                .revenueChangePercent(revenueChangePercent)
                .orders(totalOrders)
                .ordersChangePercent(ordersChangePercent)
                .build();
    }

    private BigDecimal getRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getAdCost(LocalDateTime from, LocalDateTime to) {
        return null;
    }

    private BigDecimal getProfit(LocalDateTime from, LocalDateTime to) {
        return null;
    }
}
