package com.nemi.report.model.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReportByDateModel extends ReportModel {
    private String reportDate; // dd/MM/yyyy
}
