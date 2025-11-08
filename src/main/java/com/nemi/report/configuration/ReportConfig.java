package com.nemi.report.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties("report")
public class ReportConfig {
    private String baseUrl;
    private ScaleConfig scale;
    private String datePattern;
    private BusinessToday businessToday;
    private CompareChart compareChart;

    @Data
    public static class ScaleConfig {
        private int vnd;
        private int other;
        private int percent;
        private int rate;
    }

    @Data
    public static class BusinessToday {
        private List<List<Integer>> hourFrame;
    }

    @Data
    public static class CompareChart {
        private int stepDays;
    }
}
