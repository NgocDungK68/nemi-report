package com.nemi.report.configuration;

import com.nemi.report.model.config.ColumnCategoryConfig;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.config.SystemViewConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

@Data
@Configuration
@ConfigurationProperties(prefix = "nemi.column-config.product")
public class ProductColumnConfig {
    private List<String> defaultColumns;
    private List<ColumnCategoryConfig> columnCategories;
    private List<SystemViewConfig> systemViews;

    public List<ColumnConfig> getListColumns() {
        return columnCategories.stream().flatMap(cate -> cate.getColumns().stream()).toList();
    }

    public List<String> getColumnCodes() {
        return columnCategories.stream()
                .flatMap(cate -> cate.getColumns().stream())
                .map(ColumnConfig::getCode)
                .filter(Objects::nonNull)
                .toList();
    }

    public ColumnConfig getColumnByCode(String code) {
        return getListColumns().stream().filter(col -> col.getCode().equals(code)).findFirst().orElse(null);
    }
}
