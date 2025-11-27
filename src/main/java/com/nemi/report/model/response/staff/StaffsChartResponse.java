package com.nemi.report.model.response.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.report.model.pojo.UserData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StaffsChartResponse {
    @JsonProperty("totalElements")
    private Integer totalElements;

    @JsonProperty("userData")
    private List<StaffChartData> userData;

    @JsonProperty("summary")
    private Summary summary;

    @Data
    public static class StaffChartData {
        @JsonProperty("user")
        private UserData user;

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
