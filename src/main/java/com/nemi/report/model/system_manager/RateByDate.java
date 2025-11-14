package com.nemi.report.model.system_manager;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RateByDate {
    private LocalDate date;
    private BigDecimal rate;
}
