package com.nemi.report.model.system_manager;

import lombok.Data;

import java.util.List;

@Data
public class ExchangeRateResponse {
    private List<RateByDate> ratesByDate;
}
