package com.nemi.report.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRates {
    LocalDateTime from;
    LocalDateTime to;
    Map<LocalDate, BigDecimal> currencyRate;

    public CurrencyRates currencyRateIn(LocalDate date) {
        CurrencyRates currencyRates = new CurrencyRates();
        if (ObjectUtils.isNotEmpty(currencyRate)) {
            currencyRates.currencyRate = Map.of(date, this.currencyRate.get(date));
        }
        currencyRates.from = date.atStartOfDay();
        currencyRates.to = date.atTime(LocalTime.MAX);
        return currencyRates;
    }
}
