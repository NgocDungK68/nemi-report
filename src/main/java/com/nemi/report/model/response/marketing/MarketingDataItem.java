package com.nemi.report.model.response.marketing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketingDataItem {
    @JsonProperty("date")
    private String date; // dd/MM/yyyy format

    @JsonProperty("extraData")
    private Map<String, Object> extraData;
}
