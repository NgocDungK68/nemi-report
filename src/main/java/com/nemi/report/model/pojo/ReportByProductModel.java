package com.nemi.report.model.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class ReportByProductModel extends ReportModel {
    private String productId; // key
    private String name;
    private String image;
}
