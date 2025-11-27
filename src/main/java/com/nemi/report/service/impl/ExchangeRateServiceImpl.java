package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.client.SystemManagerClient;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.system_manager.ExchangeRateResponse;
import com.nemi.report.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final SystemManagerClient systemManagerClient;

    @Override
    public ExchangeRateResponse getExchangeRate(Integer companyId, String currentCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate) {
        String startDateStr = startDate.toString();
        String endDateStr = endDate.toString();

        ExchangeRateResponse exchangeRateResponse = systemManagerClient.getExchangeRate(
                companyId, currentCurrency, targetCurrency, startDateStr, endDateStr).getBody();

        if (exchangeRateResponse == null || exchangeRateResponse.getRatesByDate() == null) {
            log.error("[getExchangeRate] Exchange rate response is null");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }

        return exchangeRateResponse;
    }

    @Override
    public BigDecimal getLastExchangeRate(Integer companyId, String currentCurrency, String targetCurrency, LocalDate startDate, LocalDate endDate) {
        ExchangeRateResponse exchangeRateResponse = getExchangeRate(companyId, currentCurrency, targetCurrency, startDate, endDate);
        return exchangeRateResponse.getRatesByDate().get(0).getRate();
    }
}
