package com.nemi.report.model.config;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.constant.ColumnDataType;
import com.nemi.report.constant.ColumnEnumData;
import com.nemi.report.model.response.nemi.SummaryData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIncludeProperties({"code", "titleEn", "titleVi", "type", "isCost", "hasFilter", "enums", "objectTypes", "updatable"})
public class ColumnConfig {
    private String code;
    private String titleEn;
    private String titleVi;
    private ColumnDataType type;
    private Boolean isCost = false;
    private ProductSource source;
    private String mapping;
    private List<String> subColumns;
    private String customMapper;
    private String customFilter;
    private String formula;
    private List<String> requiredForAvg;
    private String avgFormula;
    private SummaryData.SummaryType summaryType = SummaryData.SummaryType.SUM;
    private Boolean hasFilter = true;
    private List<ColumnEnumData> enums;
    private Boolean updatable = false;
    private Boolean isUseCent = false;
    private Boolean isPercent;
}
