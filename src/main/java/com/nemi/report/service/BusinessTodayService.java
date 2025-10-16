package com.nemi.report.service;

import com.nemi.report.model.request.BusinessTodayRequest;
import com.nemi.report.model.response.BusinessTodayResponse;

public interface BusinessTodayService {
    BusinessTodayResponse getBusinessToday(BusinessTodayRequest request);
}
