package com.nemi.report.configuration;

import com.nemi.report.constant.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {
    private final CurrencyConfig currencyConfig;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("currencyRestTemplate")
    public RestTemplate currencyRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(
                currencyConfig.getBaseUrl() + "/" +
                        ApiPath.SERVICE_API + "/" +
                        currencyConfig.getVersion() + "/"
        ));
        return restTemplate;
    }
}
