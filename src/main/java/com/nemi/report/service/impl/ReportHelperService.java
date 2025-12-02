package com.nemi.report.service.impl;

import com.nemi.constant.CurrencyCodeEnum;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.config.SummaryData;
import com.nemi.report.model.pojo.FullOrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByHourModel;
import com.nemi.report.model.pojo.ReportByDateModel;
import com.nemi.report.model.pojo.ReportModel;
import com.nemi.report.model.system_manager.ExchangeRateResponse;
import com.nemi.report.service.ExchangeRateService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.CurrencyUtils;
import com.nemi.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportHelperService {

    private final ExchangeRateService exchangeRateService;
    private final ClaimUtil claimUtil;

    private static final int DECIMAL_SCALE = 2;
    private static final int REVENUE_DECIMAL_SCALE = 4;

    public void exchangeRevenue(List<OrderQueryByDateModel> orderQueryModel, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate) {
        ExchangeRateResponse exchangeRateResponse = exchangeRateService.getExchangeRate(
                claimUtil.getCompanyId(),
                CurrencyCodeEnum.VND.name(),
                currency.name(),
                startDate,
                endDate
        );

        Map<String, BigDecimal> rateByDateMap = buildExchangeRateMap(exchangeRateResponse);
        applyExchangeRatesToOrders(orderQueryModel, rateByDateMap, currency);
    }

    public void exchangeRevenueForFullOrders(List<FullOrderQueryByDateModel> orderQueryModel, CurrencyCodeEnum currency, LocalDate startDate, LocalDate endDate) {
        if (!orderQueryModel.isEmpty()) {
            ExchangeRateResponse exchangeRateResponse = exchangeRateService.getExchangeRate(
                    claimUtil.getCompanyId(),
                    CurrencyCodeEnum.VND.name(),
                    currency.name(),
                    startDate,
                    endDate
            );

            Map<String, BigDecimal> rateByDateMap = buildExchangeRateMap(exchangeRateResponse);
            applyExchangeRatesToFullOrders(orderQueryModel, rateByDateMap, currency);
        }
    }

    public void exchangeRevenueForTodayOrders(List<OrderQueryByHourModel> orderQueryModel, CurrencyCodeEnum currency) {
        if (!orderQueryModel.isEmpty()) {
            BigDecimal todayRate = exchangeRateService.getLastExchangeRate(
                    claimUtil.getCompanyId(),
                    CurrencyCodeEnum.VND.name(),
                    currency.name(),
                    DateUtils.vietnamToday(),
                    DateUtils.vietnamToday()
            );

            orderQueryModel.forEach(orderQuery -> orderQuery.setTrueRevenue(CurrencyUtils.exchange(currency, orderQuery.getTrueRevenue(), todayRate)));
        }

    }

    public <T extends ReportModel> Object getValueFromReportModel(T reportModel, String code) {
        if (code == null) {
            return null;
        }

        return switch (code) {
            case "orders" -> reportModel.getOrders();
            case "confirmed_orders" -> reportModel.getConfirmedOrders();
            case "returned_orders" -> reportModel.getReturnedOrders();
            case "success_orders" -> reportModel.getSuccessOrders();
            case "revenue" -> reportModel.getRevenue();
            case "true_revenue" -> reportModel.getTrueRevenue();
            case "profit" -> reportModel.getProfit();
            case "ad_cost" -> reportModel.getAdCost();
            case "ad_cost_per_order" -> reportModel.getAdCostPerOrder();
            case "ads_cost_per_confirmed_order" -> reportModel.getAdCostPerConfirmedOrder();
            case "ad_cost_per_revenue" -> reportModel.getAdCostPerRevenue();
            default -> null;
        };
    }

    public Object calculateSum(List<? extends ReportModel> reportModels, String mapping) {
        return switch (mapping) {
            case "orders" -> reportModels.stream().mapToLong(ReportModel::getOrders).sum();
            case "confirmed_orders" -> reportModels.stream().mapToLong(ReportModel::getConfirmedOrders).sum();
            case "returned_orders" -> reportModels.stream().mapToLong(ReportModel::getReturnedOrders).sum();
            case "success_orders" -> reportModels.stream().mapToLong(ReportModel::getSuccessOrders).sum();
            case "revenue" -> reportModels.stream()
                    .map(ReportModel::getRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "true_revenue" -> reportModels.stream()
                    .map(ReportModel::getTrueRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "profit" -> reportModels.stream()
                    .map(ReportModel::getProfit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case "ad_cost" -> reportModels.stream()
                    .map(ReportModel::getAdCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            default -> null;
        };
    }

    public <T extends ReportModel> Object calculateAverage(List<T> reportModels, String mapping) {
        if (reportModels.isEmpty()) {
            return 0.0;
        }

        return switch (mapping) {
            case "ad_cost_per_order" -> {
                BigDecimal sum = reportModels.stream()
                        .map(ReportModel::getAdCostPerOrder)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(reportModels.size()), DECIMAL_SCALE, RoundingMode.HALF_UP);
            }
            case "ads_cost_per_confirmed_order" -> {
                BigDecimal sum = reportModels.stream()
                        .map(ReportModel::getAdCostPerConfirmedOrder)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(reportModels.size()), DECIMAL_SCALE, RoundingMode.HALF_UP);
            }
            case "ad_cost_per_revenue" -> {
                double sum = reportModels.stream()
                        .mapToDouble(ReportModel::getAdCostPerRevenue)
                        .sum();
                yield sum / reportModels.size();
            }
            default -> null;
        };
    }

    public BigDecimal calculateAdCostPerOrder(BigDecimal adCost, Long orders) {
        if (orders > 0) {
            return adCost.divide(BigDecimal.valueOf(orders), DECIMAL_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public Double calculateAdCostPerRevenue(BigDecimal adCost, BigDecimal revenue) {
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            return adCost.divide(revenue, REVENUE_DECIMAL_SCALE, RoundingMode.HALF_UP).doubleValue();
        }
        return 0.0;
    }

    public <T extends ReportModel> Map<String, Object> buildSummary(List<T> reportModels, List<ColumnConfig> columnConfigs) {
        Map<String, Object> summary = new HashMap<>();

        for (ColumnConfig columnConfig : columnConfigs) {
            String code = columnConfig.getCode();
            String mapping = columnConfig.getMapping();

            if (mapping == null) {
                continue;
            }

            // Calculate summary based on summary type (SUM or AVG)
            Object summaryValue;
            if (columnConfig.getSummaryType() == null || columnConfig.getSummaryType().equals(SummaryData.SummaryType.SUM)) {
                summaryValue = calculateSum(reportModels, mapping);
            } else {
                summaryValue = calculateAverage(reportModels, mapping);
            }

            summary.put(code, summaryValue);
        }

        return summary;
    }

    public <T extends ReportModel> Map<String, Object> buildExtraData(T reportModel, List<ColumnConfig> columnConfigs) {
        Map<String, Object> extraData = new HashMap<>();

        for (ColumnConfig columnConfig : columnConfigs) {
            String code = columnConfig.getCode();
            Object value = getValueFromReportModel(reportModel, code);
            extraData.put(code, value);
        }

        return extraData;
    }

    public <T extends ReportModel> List<T> paginateReportModels(List<T> reportModels, int page, int size) {
        int totalElements = reportModels.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        return reportModels.subList(fromIndex, toIndex);
    }

    public void populateOrderDataByDate(ReportByDateModel reportModel, OrderQueryByDateModel orderQuery) {
        if (orderQuery != null) {
            reportModel.setOrders(orderQuery.getOrders());
            reportModel.setConfirmedOrders(orderQuery.getConfirmedOrders());
            reportModel.setReturnedOrders(orderQuery.getReturnedOrders());
            reportModel.setSuccessOrders(orderQuery.getSuccessOrders());
            reportModel.setRevenue(orderQuery.getRevenue());
            reportModel.setTrueRevenue(orderQuery.getTrueRevenue());
        } else {
            reportModel.setOrders(0L);
            reportModel.setConfirmedOrders(0L);
            reportModel.setReturnedOrders(0L);
            reportModel.setSuccessOrders(0L);
            reportModel.setRevenue(BigDecimal.ZERO);
            reportModel.setTrueRevenue(BigDecimal.ZERO);
        }
    }

    public void populateFullOrderDataByDate(ReportByDateModel reportModel, FullOrderQueryByDateModel orderQuery) {
        if (orderQuery != null) {
            reportModel.setOrders(orderQuery.getOrders());
            reportModel.setConfirmedOrders(orderQuery.getConfirmedOrders());
            reportModel.setReturnedOrders(orderQuery.getReturnedOrders());
            reportModel.setRevenue(orderQuery.getRevenue());
            reportModel.setTrueRevenue(orderQuery.getTrueRevenue());
            reportModel.setReturnedRevenue(orderQuery.getReturnedRevenue());
        } else {
            reportModel.setOrders(0L);
            reportModel.setConfirmedOrders(0L);
            reportModel.setReturnedOrders(0L);
            reportModel.setRevenue(BigDecimal.ZERO);
            reportModel.setTrueRevenue(BigDecimal.ZERO);
            reportModel.setReturnedRevenue(BigDecimal.ZERO);
        }
    }

    // private

    private Map<String, BigDecimal> buildExchangeRateMap(ExchangeRateResponse exchangeRateResponse) {
        Map<String, BigDecimal> rateByDateMap = new HashMap<>();
        for (var rateByDate : exchangeRateResponse.getRatesByDate()) {
            String dateStr = DateUtils.dateToString(rateByDate.getDate());
            rateByDateMap.put(dateStr, rateByDate.getRate());
        }
        return rateByDateMap;
    }

    private void applyExchangeRatesToOrders(List<OrderQueryByDateModel> orderQueryModel, Map<String, BigDecimal> rateByDateMap, CurrencyCodeEnum currency) {
        orderQueryModel.forEach(order -> {
            BigDecimal rate = rateByDateMap.getOrDefault(order.getReportDate(), BigDecimal.ONE);

            if (order.getRevenue() != null) {
                order.setRevenue(CurrencyUtils.exchange(currency, order.getRevenue(), rate));
            }
            if (order.getTrueRevenue() != null) {
                order.setTrueRevenue(CurrencyUtils.exchange(currency, order.getTrueRevenue(), rate));
            }
        });
    }

    private void applyExchangeRatesToFullOrders(List<FullOrderQueryByDateModel> orderQueryModel, Map<String, BigDecimal> rateByDateMap, CurrencyCodeEnum currency) {
        orderQueryModel.forEach(order -> {
            BigDecimal rate = rateByDateMap.getOrDefault(order.getReportDate(), BigDecimal.ONE);

            // Apply exchange rate to all revenue fields
            if (order.getRevenue() != null) {
                order.setRevenue(CurrencyUtils.exchange(currency, order.getRevenue(), rate));
            }
            if (order.getTrueRevenue() != null) {
                order.setTrueRevenue(CurrencyUtils.exchange(currency, order.getTrueRevenue(), rate));
            }
            if (order.getConfirmedRevenue() != null) {
                order.setConfirmedRevenue(CurrencyUtils.exchange(currency, order.getConfirmedRevenue(), rate));
            }
            if (order.getReturnedRevenue() != null) {
                order.setReturnedRevenue(CurrencyUtils.exchange(currency, order.getReturnedRevenue(), rate));
            }
            if (order.getDeliveringRevenue() != null) {
                order.setDeliveringRevenue(CurrencyUtils.exchange(currency, order.getDeliveringRevenue(), rate));
            }
        });
    }
}
