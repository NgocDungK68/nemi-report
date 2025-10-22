package com.nemi.report.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties("report")
public class ReportConfig {
    private ScaleConfig scale;
    private String datePattern;
    private BusinessToday businessToday;
    private List<List<Integer>> hourFrame;

    @Data
    public static class ScaleConfig {
        private int changePercent;
        private int amount;
    }

    @Data
    public static class BusinessToday {
        private List<List<Integer>> hourFrame;
    }
}
