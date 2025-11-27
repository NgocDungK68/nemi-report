package com.nemi.report.service;

import com.nemi.report.model.system_manager.ExchangeRateResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRateService {

    ExchangeRateResponse getExchangeRate(Integer companyId, String currentCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate);

    BigDecimal getLastExchangeRate(Integer companyId, String currentCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate);

}
