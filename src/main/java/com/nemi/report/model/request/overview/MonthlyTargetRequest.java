package com.nemi.report.model.request.overview;

import com.nemi.constant.CurrencyCodeEnum;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class MonthlyTargetRequest {

    @NotNull
    private CurrencyCodeEnum currency; // VND/USD
}
