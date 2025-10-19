package com.nemi.report.service.impl;

import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.model.request.OverviewReportRequest;
import com.nemi.report.model.response.OverviewReportResponse;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.OverviewReportService;
import com.nemi.report.util.ReportUtil;
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

        LocalDateTime from = request.getFrom().atStartOfDay();
        LocalDateTime to = request.getTo().atTime(LocalTime.MAX);
        LocalDateTime compareFrom = from.minusDays(request.getCompareWith().getDays());
        LocalDateTime compareTo = to.minusDays(request.getCompareWith().getDays());

        try {
            List<OrderEntity> orders = orderRepository.findByCreatedAtBetween(from, to);
            List<OrderEntity> ordersBefore = orderRepository.findByCreatedAtBetween(compareFrom, compareTo);
            List<OrderEntity> returnedOrders = orderRepository.findByStatusInAndCreatedAtBetween(returnOrdersStatus, from, to);
            List<OrderEntity> returnedOrdersBefore = orderRepository.findByStatusInAndCreatedAtBetween(returnOrdersStatus, compareFrom, compareTo);
            List<OrderEntity> confirmedOrders = orderRepository.findByStatusInAndCreatedAtBetween(confirmedOrderStatus, from, to);
            List<OrderEntity> confirmedOrdersBefore = orderRepository.findByStatusInAndCreatedAtBetween(confirmedOrderStatus, compareFrom, compareTo);
            List<OrderEntity> deliveringOrders = orderRepository.findByStatusInAndCreatedAtBetween(deliveringOrderStatus, from, to);
            List<OrderEntity> deliveringOrdersBefore = orderRepository.findByStatusInAndCreatedAtBetween(deliveringOrderStatus, compareFrom, compareTo);

            OverviewReportResponse.OrderData totalRevenueData = getOrderData(orders, ordersBefore);
            OverviewReportResponse.OrderData returnedOrdersData = getOrderData(returnedOrders, returnedOrdersBefore);
            OverviewReportResponse.OrderData confirmedOrdersData = getOrderData(confirmedOrders, confirmedOrdersBefore);
            OverviewReportResponse.OrderData deliveringOrdersData = getOrderData(deliveringOrders, deliveringOrdersBefore);
            OverviewReportResponse.CostData adCostData = getAdCostData();
            OverviewReportResponse.ProfitData profitData = getProfitData(
                    totalRevenueData.getRevenue(), getRevenue(ordersBefore),
                    returnedOrdersData.getRevenue(), getRevenue(returnedOrdersBefore),
                    adCostData.getCost(), getCostBefore()
            );

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

    private OverviewReportResponse.OrderData getOrderData(List<OrderEntity> orders, List<OrderEntity> ordersBefore) {
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

    /**
     * Lợi Nhuận = Doanh số - ( chi phí ads + Doanh số đơn hoàn )
     */
    private OverviewReportResponse.ProfitData getProfitData(BigDecimal totalRevenue, BigDecimal totalRevenueBefore,
                                                            BigDecimal returnedOrdersRevenue, BigDecimal returnedOrdersRevenueBefore,
                                                            BigDecimal adCost, BigDecimal adCostBefore) {
        // Lợi nhuận hiện tại & trước đó
        BigDecimal profitValue = totalRevenue.subtract(returnedOrdersRevenue.add(adCost));
        BigDecimal profitValueBefore = totalRevenueBefore.subtract(returnedOrdersRevenueBefore.add(adCostBefore));

        // % thay đổi lợi nhuận
        BigDecimal profitChangePercent = reportUtil.changePercent(profitValue, profitValueBefore);

        return OverviewReportResponse.ProfitData.builder()
                .value(profitValue)
                .changePercent(profitChangePercent)
                .build();
    }

    private OverviewReportResponse.CostData getAdCostData() {
        return null;
    }

    private BigDecimal getCostBefore() {
        return BigDecimal.ZERO;
    }

    private BigDecimal getRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
