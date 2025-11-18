package com.nemi.report.model.ads_manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdsCostOfDepartmentRequest {
    private Integer companyId;
    private String departmentId;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
}
