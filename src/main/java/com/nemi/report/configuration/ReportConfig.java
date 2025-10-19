package com.nemi.report.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties("report")
public class ReportConfig {
    private ScaleConfig scale;
    private BigDecimal exchangeRate;

    @Data
    public static class ScaleConfig {
        private int changePercent;
        private int amount;
    }
}
