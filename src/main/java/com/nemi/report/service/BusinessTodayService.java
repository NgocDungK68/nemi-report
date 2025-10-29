package com.nemi.report.service;

import com.nemi.report.model.request.overview.BusinessTodayRequest;
import com.nemi.report.model.response.overview.BusinessTodayResponse;

public interface BusinessTodayService {
    BusinessTodayResponse getBusinessToday(BusinessTodayRequest request);
}
