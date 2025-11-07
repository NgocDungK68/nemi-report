package com.nemi.report.model.request;

import com.nemi.report.constant.OrderDirection;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnRequest {
    @NotNull
    private String code;
    private OrderDirection order;
}
