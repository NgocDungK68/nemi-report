package com.nemi.report.model.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColumnCategoryConfig {
    private String categoryNameEn;
    private String categoryNameVi;
    private List<ColumnConfig> columns;
}
