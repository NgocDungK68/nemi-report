package com.nemi.report.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties("currency")
public class CurrencyConfig {
    private String baseUrl;
    private String version;
    private String currencyRateUrl;
    private List<String> type;
}
