package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.configuration.CurrencyConfig;
import com.nemi.report.configuration.ReportConfig;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.entity.MonthlyTargetEntity;
import com.nemi.report.entity.MonthlyTargetId;
import com.nemi.report.entity.OrderEntity;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.overview.MonthlyTargetRequest;
import com.nemi.report.model.request.overview.UpdateMonthlyTargetRequest;
import com.nemi.report.model.response.CurrencyRateResponse;
import com.nemi.report.model.response.overview.ConfigResponse;
import com.nemi.report.model.response.overview.MonthlyTargetResponse;
import com.nemi.report.model.response.overview.RevenueSummary;
import com.nemi.report.repository.MonthlyTargetRepository;
import com.nemi.report.repository.OrderItemRepository;
import com.nemi.report.repository.OrderRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.report.service.MonthlyTargetService;
import com.nemi.report.util.DateUtils;
import com.nemi.util.ClaimUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyTargetServiceImpl implements MonthlyTargetService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BusinessTodayServiceImpl businessTodayService;
    private final OverviewReportServiceImpl overviewReportService;
    private final MonthlyTargetRepository monthlyTargetRepository;
    private final ReportConfig reportConfig;
    private final ClaimUtil claimUtil;
    private final ConfigService configService;
    private final CurrencyRateService currencyRateService;
    private final CurrencyConfig currencyConfig;

    private int percentScale;

    @PostConstruct
    public void init() {
        percentScale = reportConfig.getScale().getPercent();
    }

    @Override
    public MonthlyTargetResponse getMonthlyTarget(MonthlyTargetRequest request) {
        log.info("[MonthlyTargetServiceImpl.getMonthlyTarget] Start calculating monthly target with currency {} for departmentId={}",
                request.getCurrency(),
                claimUtil.getDepartmentId());

        try {
            CurrencyRates currencyRates = currencyRateService.getCurrencyRatesThisMonth(
                    claimUtil.getCompanyId(),
                    request.getCurrency()
            );

            ConfigResponse config = configService.getConfig();

            // KPI
            MonthlyTargetId monthlyTargetId = new MonthlyTargetId(claimUtil.getDepartmentId(), request.getCurrency().getCode());
            Optional<MonthlyTargetEntity> monthlyTargetEntity = monthlyTargetRepository.findById(monthlyTargetId);
            BigDecimal targetRevenue = monthlyTargetEntity.map(MonthlyTargetEntity::getRevenue).orElse(BigDecimal.ZERO);
            BigDecimal targetAdCostPerRevenue = monthlyTargetEntity.map(MonthlyTargetEntity::getAdCostPerRevenue).orElse(BigDecimal.ZERO);
            BigDecimal targetReturnedOrderPercent = monthlyTargetEntity.map(MonthlyTargetEntity::getReturnedOrderPercent).orElse(BigDecimal.ZERO);

            // Doanh số hôm nay
            BigDecimal revenueToday = businessTodayService.getRevenueToday(request.getCurrency());

            // Thông tin doanh số và số lượng orders trong tháng tính đến thời điểm hiện tại
            LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            List<OrderEntity> ordersThisMonth = orderRepository.findByStatusInAndUpdatedAtBetween(
                    OrderStatus.getTotalOrdersStatus(),
                    firstDayOfMonth,
                    LocalDate.now().atTime(LocalTime.MAX)
            );
            log.debug("[MonthlyTargetServiceImpl.getMonthlyTarget] Found {} orders this month", ordersThisMonth.size());

            RevenueSummary orderThisMonth = overviewReportService.getOrderSummary(OrderStatus.getTotalOrdersStatus(), currencyRates);
            BigDecimal totalRevenue = orderThisMonth.getRevenue();
            BigDecimal totalOrders = orderThisMonth.getNumber();
            RevenueSummary returnedOrder = overviewReportService.getOrderSummary(config.getReturnOrderWhen().getOrderStatus(), currencyRates);

            // Thông tin ads
            RevenueSummary ads = overviewReportService.getAdsSummary(currencyRates);
            BigDecimal adCost = ads.getRevenue();

            // Build Response
            MonthlyTargetResponse monthlyTargetResponse = MonthlyTargetResponse.builder()
                    .totalRevenue(totalRevenue)
                    .todayRevenue(revenueToday)
                    .todayRevenuePercent(getTodayRevenuePercent(revenueToday, targetRevenue))
                    .remainingRevenueNeeded(getRemainingRevenueNeeded(totalRevenue, targetRevenue))
                    .adCost(adCost)
                    .adCostPerRevenue(ads.getNumber())
                    .targetAdCostPerRevenue(targetAdCostPerRevenue)
                    .adCostPerOrder(getAdCostPerOrder(adCost, totalOrders))
                    .order(totalOrders)
                    .soldProduct(getSoldProduct(ordersThisMonth))
                    .returnedOrder(returnedOrder.getNumber())
                    .returnedOrderPercent(getReturnedOrderPercent(returnedOrder.getNumber(), totalOrders))
                    .targetReturnedOrderPercent(targetReturnedOrderPercent)
                    .targetRevenue(targetRevenue)
                    .targetRevenueProcess(getTargetRevenueProcess(totalRevenue, targetRevenue))
                    .build();

            log.info("[MonthlyTargetServiceImpl.getMonthlyTarget] Successfully built monthly target response for departmentId={}", claimUtil.getDepartmentId());
            return monthlyTargetResponse;
        } catch (Exception e) {
            log.error("[MonthlyTargetServiceImpl.getMonthlyTarget] Failed to get monthly target: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.MONTHLY_TARGET_ERROR));
        }
    }


    @Override
    public void updateMonthlyTarget(UpdateMonthlyTargetRequest request) {
        log.info("[MonthlyTargetServiceImpl.updateMonthlyTarget] Updating monthly target for departmentId={}", claimUtil.getDepartmentId());

        try {
            List<CurrencyRateResponse.ExchangeRate> exchangeRates = currencyRateService.getCurrencyRateToday(claimUtil.getCompanyId(), LocalDate.now());
            List<MonthlyTargetEntity> monthlyTargetList = new ArrayList<>();
            List<String> currencyList = new ArrayList<>();
            for (String currency : currencyConfig.getType()) {
                Optional<MonthlyTargetEntity> result = processCurrencyTarget(request, exchangeRates, currency);
                result.ifPresent(entity -> {
                    monthlyTargetList.add(entity);
                    currencyList.add(currency);
                });
            }

            monthlyTargetRepository.saveAll(monthlyTargetList);
            log.info("[MonthlyTargetServiceImpl.updateMonthlyTarget] Successfully updated monthly target for departmentId={}, currencies={}", claimUtil.getDepartmentId(), currencyList);
        } catch (Exception e) {
            log.error("[MonthlyTargetServiceImpl.updateMonthlyTarget] Failed to update monthly target: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.MONTHLY_TARGET_ERROR));
        }
    }

    /**
     * % doanh số hôm nay
     *
     * @param revenueToday  doanh số đạt được ngày hôm nay
     * @param targetRevenue doanh số mục tiêu cả tháng (KPI doanh số)
     * @return [revenueToday / (targetRevenue / số ngày trong tháng)] * 100
     */
    private BigDecimal getTodayRevenuePercent(BigDecimal revenueToday, BigDecimal targetRevenue) {
        int scale = reportConfig.getScale().getPercent() + 2;
        BigDecimal targetRevenuePerDay = targetRevenue.divide(
                BigDecimal.valueOf(DateUtils.getDaysInMonth()), scale, RoundingMode.HALF_UP
        );
        if (revenueToday.compareTo(targetRevenuePerDay) >= 0) return BigDecimal.valueOf(100);

        return revenueToday
                .divide(targetRevenue.divide(
                                BigDecimal.valueOf(DateUtils.getDaysInMonth()), scale, RoundingMode.HALF_UP
                        ),
                        scale, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(percentScale, RoundingMode.HALF_UP);
    }

    /**
     * Doanh số còn lại cần đạt
     *
     * @param totalRevenue  doanh số đạt được trong tháng
     * @param targetRevenue doanh số mục tiêu cả tháng (KPI doanh số)
     * @return targetRevenue - totalRevenue
     */
    private BigDecimal getRemainingRevenueNeeded(BigDecimal totalRevenue, BigDecimal targetRevenue) {
        if (totalRevenue.compareTo(targetRevenue) >= 0) return BigDecimal.ZERO;
        return targetRevenue.subtract(totalRevenue);
    }

    /**
     * % đơn hoàn trong tổng đơn hàng
     *
     * @param returnedOrders số lượng đơn hoàn
     * @param totalOrders    tổng đơn hàng trong tháng tính đến thời điểm hiện tại
     * @return returnedOrders / totalOrders * 100 (%)
     */
    private BigDecimal getReturnedOrderPercent(BigDecimal returnedOrders, BigDecimal totalOrders) {
        if (totalOrders.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        return returnedOrders
                .divide(
                        totalOrders, reportConfig.getScale().getPercent() + 2, RoundingMode.HALF_UP
                )
                .multiply(BigDecimal.valueOf(100))
                .setScale(percentScale, RoundingMode.HALF_UP);
    }

    private BigDecimal getAdCostPerOrder(BigDecimal adCost, BigDecimal totalOrders) {
        if (totalOrders.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return adCost.divide(
                totalOrders,
                reportConfig.getScale().getPercent() + 2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal getSoldProduct(List<OrderEntity> orders) {
        if (ObjectUtils.isEmpty(orders)) return BigDecimal.ZERO;

        List<String> orderIds = orders.stream()
                .map(OrderEntity::getOrderId)
                .filter(Objects::nonNull)
                .toList();

        if (orderIds.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalQuantity = orderItemRepository.sumQuantityByOrderIds(orderIds);
        return ObjectUtils.isNotEmpty(totalQuantity) ? totalQuantity : BigDecimal.ZERO;
    }

    /**
     * Tiến độ doanh số trong tháng
     *
     * @param totalRevenue  doanh số đạt được trong tháng
     * @param targetRevenue doanh số mục tiêu cả tháng (KPI doanh số)
     * @return totalRevenue / targetRevenue * 100 (%)
     */
    private BigDecimal getTargetRevenueProcess(BigDecimal totalRevenue, BigDecimal targetRevenue) {
        if (totalRevenue.compareTo(targetRevenue) >= 0) return BigDecimal.valueOf(100);
        return totalRevenue
                .divide(targetRevenue, reportConfig.getScale().getPercent() + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(percentScale, RoundingMode.HALF_UP);
    }

    private Optional<MonthlyTargetEntity> processCurrencyTarget(UpdateMonthlyTargetRequest request,
                                                                List<CurrencyRateResponse.ExchangeRate> exchangeRates,
                                                                String currency) {
        try {
            MonthlyTargetId monthlyTargetId = new MonthlyTargetId(claimUtil.getDepartmentId(), currency);
            Optional<MonthlyTargetEntity> monthlyTargetEntity = monthlyTargetRepository.findById(monthlyTargetId);

            MonthlyTargetEntity monthlyTarget;
            monthlyTarget = monthlyTargetEntity.orElseGet(() -> MonthlyTargetEntity.builder()
                    .departmentId(claimUtil.getDepartmentId())
                    .currency(currency)
                    .companyId(claimUtil.getCompanyId())
                    .createdBy(claimUtil.getUserName())
                    .build());

            monthlyTarget.setRevenue(getValueWithCurrency(request.getTargetRevenue(), currency, exchangeRates));
            monthlyTarget.setAdCostPerRevenue(getValueWithCurrency(request.getTargetAdCostPerRevenue(), currency, exchangeRates));
            monthlyTarget.setReturnedOrderPercent(getValueWithCurrency(request.getTargetReturnedOrderPercent(), currency, exchangeRates));
            monthlyTarget.setUpdatedBy(claimUtil.getUserName());
            monthlyTarget.setUpdatedAt(LocalDateTime.now());

            return Optional.of(monthlyTarget);
        } catch (Exception e) {
            log.error("[MonthlyTargetServiceImpl.processCurrencyTarget] Failed to update target for currency {}: {}", currency, e.getMessage(), e);
            return Optional.empty(); // bỏ qua currency lỗi
        }
    }


    private BigDecimal getValueWithCurrency(BigDecimal value,
                                            String currency,
                                            List<CurrencyRateResponse.ExchangeRate> exchangeRates) {
        if (currency.equals(Currency.VND.getCode())) {
            return value;
        } else {
            CurrencyRateResponse.ExchangeRate exchangeRate = currencyRateService.getExchangeRate(currency, exchangeRates);
            BigDecimal rate = exchangeRate.getRate();
            if (!currency.equals(Currency.USD.getCode())) {
                BigDecimal vndRate = currencyRateService.getExchangeRate(Currency.VND.getCode(), exchangeRates).getRate();
                rate = vndRate.divide(rate, reportConfig.getScale().getRate(), RoundingMode.HALF_UP);
            }

            return value.multiply(rate);
        }
    }
}
