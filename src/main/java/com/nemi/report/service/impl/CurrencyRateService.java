package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.response.PageResponse;
import com.nemi.report.configuration.CurrencyConfig;
import com.nemi.report.constant.Currency;
import com.nemi.report.constant.ReportConstants;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.CurrencyRates;
import com.nemi.report.model.request.ReportTimeRange;
import com.nemi.report.model.response.CurrencyRateResponse;
import com.nemi.util.DateUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateService {
    @Resource(name = "currencyRestTemplate")
    private final RestTemplate restTemplate;

    private final CurrencyConfig currencyConfig;

    @Value("${secret.service-username}")
    private String serviceUsername;

    @Value("${secret.service-password}")
    private String servicePassword;

    public PageResponse<CurrencyRateResponse> getCurrencyRate(Integer companyId, LocalDate from, LocalDate to) {
        try {
            String relativeUri = UriComponentsBuilder.fromPath(currencyConfig.getCurrencyRateUrl())
                    .queryParam(ReportConstants.COMPANY_ID, companyId)
                    .queryParam(ReportConstants.FROM, from.format(DateTimeFormatter.ISO_DATE))
                    .queryParam(ReportConstants.TO, to.format(DateTimeFormatter.ISO_DATE))
                    .toUriString();

            log.debug("[CurrencyRateService.getCurrencyRate] Calling URL: {}]", restTemplate.getUriTemplateHandler().expand(relativeUri));

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(serviceUsername, servicePassword);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<PageResponse<CurrencyRateResponse>> response =
                    restTemplate.exchange(relativeUri, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                            }
                    );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Currency API returned non-2xx status: {}", response.getStatusCode());
                throw new RuntimeException("Currency API returned non-2xx status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    public Map<LocalDate, BigDecimal> getCurrencyRate(Integer companyId, LocalDate from, LocalDate to, Currency currency) {
//        if (currency.equals(Currency.VND)) {
//            return null;
//        }
//
//        try {
//            List<CurrencyRateResponse> currencyRateResponses = getCurrencyRate(companyId, from, to).getData();
//
//            return currencyRateResponses.stream()
//                    .collect(Collectors.toMap(
//                            rate -> LocalDate.parse(rate.getDate(), DateTimeFormatter.ofPattern(currencyConfig.getDatePattern())),
//                            rate -> rate.getExchangeRates().stream()
//                                    .filter(er -> er.getCurrency().equals(Currency.VND.getCode()))
//                                    .findFirst()
//                                    .map(CurrencyRateResponse.ExchangeRate::getRate)
//                                    .orElseThrow(() ->
//                                            new TechnicalException(AlertMessages.alert(TechnicalAlertCode.CURRENCY_RATE_ERROR))))
//                    );
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public CurrencyRates getCurrencyRate(Integer companyId, LocalDate from, LocalDate to, Currency currency) {
        Map<LocalDate, BigDecimal> currencyRate;

        // Nếu là VND thì không cần quy đổi
        if (currency.equals(Currency.VND)) {
            currencyRate = null;
        } else {
            try {
                List<CurrencyRateResponse> currencyRateResponses = getCurrencyRate(companyId, from, to).getData();
                currencyRate = currencyRateResponses.stream()
                        .collect(Collectors.toMap(
                                rate -> LocalDate.parse(rate.getDate(), DateUtils.YYYYMMDD_FORMATER),
                                rate -> rate.getExchangeRates().stream()
                                        .filter(er -> er.getCurrency().equals(Currency.VND.getCode()))
                                        .findFirst()
                                        .map(CurrencyRateResponse.ExchangeRate::getRate)
                                        .orElseThrow(() ->
                                                new TechnicalException(AlertMessages.alert(TechnicalAlertCode.CURRENCY_RATE_ERROR))))
                        );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return CurrencyRates.builder()
                .from(from)
                .to(to)
                .currencyRate(currencyRate)
                .build();
    }
}
