package com.nemi.report.model.response.marketing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MarketingSummaryResponse {

    @JsonProperty("totalElements")
    private Long totalElements;

    @JsonProperty("totalPages")
    private Integer totalPages;

    @JsonProperty("data")
    private List<DataItem> data;

    @JsonProperty("summary")
    private Map<String, Object> summary;

    @Data
    public static class DataItem {
        @JsonProperty("date")
        private String date; // dd/MM/yyyy format

        @JsonProperty("extraData")
        private Map<String, Object> extraData;
    }
}
