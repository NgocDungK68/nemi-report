package com.nemi.report.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReportModel {
    private String productId;
    private String productName;
    private String productImage;

    private String reportDate;

    private Map<String, Object> extraData;
}
