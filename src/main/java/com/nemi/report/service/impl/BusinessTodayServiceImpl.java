package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.overview.BusinessTodayRequest;
import com.nemi.report.model.response.overview.BusinessTodayResponse;
import com.nemi.report.model.response.overview.ConfigResponse;
import com.nemi.report.model.response.overview.RevenueSummary;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.BusinessTodayService;
import com.nemi.report.service.ConfigService;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessTodayServiceImpl implements BusinessTodayService {
    private final ReportConfig reportConfig;
    private final OrderRepository orderRepository;
    private final OverviewReportServiceImpl overviewReportService;
    private final ConfigService configService;
    private final CurrencyRateService currencyRateService;
    private final ClaimUtil claimUtil;

    @Override
    public BusinessTodayResponse getBusinessToday(BusinessTodayRequest request) {
        log.info("[BusinessTodayServiceImpl.getBusinessToday] Start generating today's business report with currency: {}", request.getCurrency());

        try {
            ConfigResponse config = configService.getConfig();
            CurrencyRates currencyRateToday = currencyRateService.getCurrencyRateToday(
                    claimUtil.getCompanyId(),
                    request.getCurrency()
            );

            // Doanh số hôm nay
            BusinessTodayResponse.OrderData totalOrders = convertToOrderData(
                    overviewReportService.getOrderSummary(OrderStatus.getTotalOrdersStatus(), currencyRateToday)
            );

            // ads
            BigDecimal adCost = overviewReportService.getAdsSummary(currencyRateToday).getRevenue();
            BigDecimal adCostPerRevenue = overviewReportService.getAdsSummary(currencyRateToday).getNumber();

            // order
            BusinessTodayResponse.OrderData confirmedOrder = convertToOrderData(
                    overviewReportService.getOrderSummary(config.getConfirmOrderWhen().getOrderStatus(), currencyRateToday)
            );
            BusinessTodayResponse.OrderData deliveredOrder = convertToOrderData(
                    overviewReportService.getOrderSummary(OrderStatus.getDeliveringOrdersStatus(), currencyRateToday)
            );
            BusinessTodayResponse.OrderData canceledOrder = convertToOrderData(
                    overviewReportService.getOrderSummary(OrderStatus.getCancelledOrdersStatus(), currencyRateToday)
            );
            BusinessTodayResponse.OrderData pendingOrder = BusinessTodayResponse.OrderData.builder()
                    .revenue(totalOrders.getRevenue().subtract(deliveredOrder.getRevenue()))
                    .orders(totalOrders.getOrders().subtract(canceledOrder.getOrders()))
                    .build();

            // Doanh thu theo khung giờ linh hoạt
            List<BusinessTodayResponse.HourFrameData> revenueFrames = new ArrayList<>();
            for (List<Integer> frame : reportConfig.getBusinessToday().getHourFrame()) {
                int startHour = frame.get(0);
                int endHour = frame.get(1);
                BigDecimal frameRevenue = getFrameRevenue(startHour, endHour, currencyRateToday);

                log.debug("[BusinessTodayServiceImpl.getBusinessToday] Hour frame {}-{}: revenue={}", startHour, endHour, frameRevenue);

                revenueFrames.add(
                        BusinessTodayResponse.HourFrameData.builder()
                                .hourFrame(startHour + "-" + endHour)
                                .value(frameRevenue)
                                .build()
                );
            }

            BusinessTodayResponse response = BusinessTodayResponse.builder()
                    .revenue(totalOrders.getRevenue())
                    .adCost(adCost)
                    .adCostPerRevenue(adCostPerRevenue)
                    .confirmedOrder(confirmedOrder)
                    .deliveredOrder(deliveredOrder)
                    .pendingOrder(pendingOrder)
                    .canceledOrder(canceledOrder)
                    .revenuePerHourFrame(revenueFrames)
                    .build();

            log.info("[BusinessTodayServiceImpl.getBusinessToday] Successfully built today's business report");
            return response;
        } catch (Exception e) {
            log.error("[BusinessTodayServiceImpl.getBusinessToday] Failed to generate today's business report: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.BUSINESS_TODAY_REPORT_ERROR));
        }
    }

    private BusinessTodayResponse.OrderData convertToOrderData(RevenueSummary revenueSummary) {
        return BusinessTodayResponse.OrderData.builder()
                .revenue(revenueSummary.getRevenue())
                .orders(revenueSummary.getNumber())
                .build();
    }

    private BigDecimal getFrameRevenue(int startHour, int endHour, CurrencyRates currencyRates) {
        LocalDate dateToday = LocalDate.now();
        LocalDateTime startOfDay = dateToday.atStartOfDay();
        LocalDateTime from = startOfDay.plusHours(startHour);
        LocalDateTime to = startOfDay.plusHours(endHour);

        List<OrderEntity> orderEntities = orderRepository.findByStatusInAndUpdatedAtBetween(OrderStatus.getTotalOrdersStatus(), from, to);
        BigDecimal orderRevenue = overviewReportService.getOrderRevenue(orderEntities);
        if (ObjectUtils.isNotEmpty(currencyRates.getCurrencyRate())) {
            orderRevenue = orderRevenue.multiply(currencyRates.getCurrencyRate().get(dateToday));
        }
        return orderRevenue;
    }

    public BigDecimal getRevenueToday(Currency currency) {
        CurrencyRates currencyRates = currencyRateService.getCurrencyRateToday(
                claimUtil.getCompanyId(),
                currency
        );

        BusinessTodayResponse.OrderData totalOrders = convertToOrderData(
                overviewReportService.getOrderSummary(OrderStatus.getTotalOrdersStatus(), currencyRates)
        );

        return totalOrders.getRevenue();
    }
}
