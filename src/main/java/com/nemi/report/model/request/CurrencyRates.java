package com.nemi.report.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRates {
    LocalDate from;
    LocalDate to;
    Map<LocalDate, BigDecimal> currencyRate;

    public CurrencyRates currencyRateIn(LocalDate date) {
        return CurrencyRates.builder()
                .from(date)
                .to(date)
                .currencyRate(Map.of(date, this.getCurrencyRate().get(date)))
                .build();
    }
}
