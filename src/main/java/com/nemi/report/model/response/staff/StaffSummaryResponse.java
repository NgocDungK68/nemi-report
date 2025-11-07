package com.nemi.report.model.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StaffSummaryResponse {
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
        @JsonProperty("user")
        private UserData user;

        @JsonProperty("extraData")
        private Map<String, Object> extraData;
    }

    @Data
    public static class UserData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("image")
        private String imageUrl;
    }
}
