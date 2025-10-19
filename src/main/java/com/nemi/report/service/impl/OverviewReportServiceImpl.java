package com.nemi.report.service.impl;

import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.model.request.OverviewReportRequest;
import com.nemi.report.model.response.OverviewReportResponse;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.OverviewReportService;
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

    @Override
    public OverviewReportResponse getOverviewReport(OverviewReportRequest request) {
        LocalDateTime from = request.getFrom().atStartOfDay();
        LocalDateTime to = request.getTo().atTime(LocalTime.MAX);

        return null;
    }

//    private OverviewReportResponse.RevenueData buildRevenueData(List<OrderEntity> orders) {
//        BigDecimal revenue = orders.stream()
//                .map(OrderEntity::getTotalPrice)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//
//        OverviewReportResponse.RevenueData data = OverviewReportResponse.RevenueData.builder()
//                .revenue(revenue)
//                .orders(BigDecimal.valueOf(orders.size()))
//                .revenueChangePercent(BigDecimal.ZERO)
//                .ordersChangePercent(BigDecimal.ZERO)
//                .build();
//
//        return data;
//    }


    private BigDecimal getTotalRevenue(LocalDateTime from, LocalDateTime to) {
        List<OrderEntity> totalOrders = orderRepository.findByCreatedAtBetween(from, to);
        return totalOrders.stream()
                .map(OrderEntity::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getReturnedOrdersRevenue(LocalDateTime from, LocalDateTime to) {
        List<String> returnedOrdersStatus = List.of(
                OrderStatus.RETURNED.getCode(),
                OrderStatus.CANCELLED.getCode(),
                OrderStatus.FAILED.getCode()
        );

        List<OrderEntity> returnedOrders = orderRepository.findByStatusInAndCreatedAtBetween(returnedOrdersStatus, from, to);
        return null;
    }

    private BigDecimal getConfirmedOrdersRevenue(LocalDateTime from, LocalDateTime to) {
        List<String> confirmedOrdersStatus = List.of(
                OrderStatus.DELIVERED.getCode(),
                OrderStatus.SHIPPING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode()
        );
        return null;
    }

    private BigDecimal getDeliveringOrdersRevenue(LocalDateTime from, LocalDateTime to) {
        List<String> deliveringOrdersStatus = List.of(
                OrderStatus.SHIPPING.getCode(),
                OrderStatus.READY_TO_SHIP.getCode()
        );
        return null;
    }

    private BigDecimal getAdCost(LocalDateTime from, LocalDateTime to) {
        return null;
    }

    private BigDecimal getProfit(LocalDateTime from, LocalDateTime to) {
        return null;
    }
}
