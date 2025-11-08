package com.nemi.report.model;

import com.nemi.report.constant.OrderDirection;
import com.nemi.report.model.config.ColumnConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderParameter {
    private ColumnConfig column;
    private OrderDirection direction;
}
