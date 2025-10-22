package com.nemi.report.service.impl;

import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.model.request.BusinessTodayRequest;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.response.BusinessTodayResponse;
import com.nemi.report.model.response.RevenueSummary;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.BusinessTodayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessTodayServiceImpl implements BusinessTodayService {
    private final OrderRepository orderRepository;
    private final ReportConfig reportConfig;
    private final OverviewReportServiceImpl overviewReportService;

    @Override
    public BusinessTodayResponse getBusinessToday(BusinessTodayRequest request) {
        ReportTimeRange timeToday = ReportTimeRange.today();

        // Doanh số hôm nay
        BusinessTodayResponse.OrderData totalOrders = convertToOrderData(
                overviewReportService.getOrderSummary(timeToday, OrderStatus.getTotalOrdersStatus())
        );

        // ads
        BigDecimal adCost = overviewReportService.getAdsSummary(timeToday).getRevenue();
        BigDecimal adCostPerRevenue = overviewReportService.getAdsSummary(timeToday).getNumber();

        // order
        BusinessTodayResponse.OrderData confirmedOrder = convertToOrderData(
                overviewReportService.getOrderSummary(timeToday, OrderStatus.getConfirmedOrdersStatus())
        );
        BusinessTodayResponse.OrderData deliveredOrder = convertToOrderData(
                overviewReportService.getOrderSummary(timeToday, OrderStatus.getDeliveringOrdersStatus())
        );
        BusinessTodayResponse.OrderData canceledOrder = convertToOrderData(
                overviewReportService.getOrderSummary(timeToday, OrderStatus.getCancelledOrdersStatus())
        );
        BusinessTodayResponse.OrderData pendingOrder = BusinessTodayResponse.OrderData.builder()
                .revenue(totalOrders.getRevenue().subtract(deliveredOrder.getRevenue()))
                .orders(totalOrders.getOrders().subtract(canceledOrder.getOrders()))
                .build();

        // Doanh thu theo khung giờ linh hoạt
        List<BusinessTodayResponse.HourFrameData> revenueFrames = new ArrayList<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        for (List<Integer> frame : reportConfig.getBusinessToday().getHourFrame()) {
            int startHour = frame.get(0);
            int endHour = frame.get(1);
            LocalDateTime from = startOfDay.plusHours(startHour);
            LocalDateTime to = startOfDay.plusHours(endHour);
            BigDecimal frameRevenue = overviewReportService.getOrderRevenue(orderRepository.findByUpdatedAtBetween(from, to));

            revenueFrames.add(
                    BusinessTodayResponse.HourFrameData.builder()
                            .hourFrame(startHour + "-" + endHour)
                            .value(frameRevenue)
                            .build()
            );
        }

        return BusinessTodayResponse.builder()
                .revenue(totalOrders.getRevenue())
                .adCost(adCost)
                .adCostPerRevenue(adCostPerRevenue)
                .confirmedOrder(confirmedOrder)
                .deliveredOrder(deliveredOrder)
                .pendingOrder(pendingOrder)
                .canceledOrder(canceledOrder)
                .revenuePerHourFrame(revenueFrames)
                .build();
    }

    private BusinessTodayResponse.OrderData convertToOrderData(RevenueSummary revenueSummary) {
        return BusinessTodayResponse.OrderData.builder()
                .revenue(revenueSummary.getRevenue())
                .orders(revenueSummary.getNumber())
                .build();
    }

    public BigDecimal getRevenueToday() {
        BusinessTodayResponse.OrderData totalOrders = convertToOrderData(
                overviewReportService.getOrderSummary(ReportTimeRange.today(), OrderStatus.getTotalOrdersStatus())
        );

        return totalOrders.getRevenue();
    }
}
