package com.nemi.report.model.response.marketing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingChartResponse {
    @JsonProperty("data")
    private List<MarketingDataItem> data;
}
