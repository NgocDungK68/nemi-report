package com.nemi.report.service;

import com.nemi.report.constant.Currency;
import com.nemi.report.model.request.overview.BusinessTodayRequest;
import com.nemi.report.model.response.overview.BusinessTodayResponse;

import java.math.BigDecimal;

public interface BusinessTodayService {
    BusinessTodayResponse getBusinessToday(BusinessTodayRequest request);
    BigDecimal getRevenueToday(Currency currency);
}
