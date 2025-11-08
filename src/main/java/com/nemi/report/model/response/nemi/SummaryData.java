package com.nemi.report.model.response.nemi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SummaryData {
    private String columnCode;
    private SummaryType type;
    private String value;

    public enum SummaryType {
        SUM,AVG,NONE;

        public boolean isHasSummary() {
            return this != NONE;
        }
    }
}