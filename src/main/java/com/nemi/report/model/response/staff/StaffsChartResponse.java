package com.nemi.report.model.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StaffsChartResponse {
    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("staffData")
    private List<StaffData> staffData;

    @JsonProperty("summary")
    private Summary summary;

    @Data
    public static class StaffData {
        @JsonProperty("product")
        private Product product;

        @JsonProperty("data")
        private DataValue data;

        @JsonProperty("dateValues")
        private List<DateValue> dateValues;
    }

    @Data
    public static class Summary {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }

    @Data
    public static class Product {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class DataValue {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }

    @Data
    public static class DateValue {
        @JsonProperty("date")
        private LocalDate date; // dd/MM/yyyy format

        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("percent")
        private BigDecimal percent;
    }
}
