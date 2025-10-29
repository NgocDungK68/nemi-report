package com.nemi.report.service.impl;

import com.nemi.report.configuration.CurrencyConfig;
import com.nemi.report.constant.ReportConstants;
import com.nemi.report.model.response.CurrencyRateResponse;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

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

    public CurrencyRateResponse getCurrencyRate(Long companyId, LocalDate from, LocalDate to) {
        try {
            String relativeUri = UriComponentsBuilder.fromPath(currencyConfig.getCurrencyRateUrl())
                    .queryParam(ReportConstants.COMPANY_ID, companyId)
                    .queryParam(ReportConstants.FROM, from)
                    .queryParam(ReportConstants.TO, to)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(serviceUsername, servicePassword);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<CurrencyRateResponse> response =
                    restTemplate.exchange(relativeUri, HttpMethod.GET, entity, CurrencyRateResponse.class);

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
}
