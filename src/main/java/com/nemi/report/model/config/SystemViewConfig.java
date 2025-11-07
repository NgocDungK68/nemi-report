package com.nemi.report.model.config;

import lombok.Data;

import java.util.List;

@Data
public class SystemViewConfig {
    private String code;
    private String titleEn;
    private String titleVi;
    private List<String> columns;
}
