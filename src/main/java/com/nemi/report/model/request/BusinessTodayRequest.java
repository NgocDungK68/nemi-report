package com.nemi.report.model.request;

import com.nemi.report.constant.Currency;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class BusinessTodayRequest {

    @NotNull
    private Currency currency; // VND/USD
}
