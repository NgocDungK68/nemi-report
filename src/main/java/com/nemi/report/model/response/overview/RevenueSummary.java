package com.nemi.report.model.response.overview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenueSummary {
    private BigDecimal revenue;
    private BigDecimal number;
}
