package com.nemi.report.model.ads_manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdsCostOfProductsRequest {
    private List<String> productIds;
    private Integer companyId;
    private String departmentId;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
}
