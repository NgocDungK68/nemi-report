package com.nemi.report.model.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class ReportByUserModel extends ReportModel {
    private String userId; // key
    private String name;
    private String image;
}
