package com.nemi.report.service.impl;

import com.nemi.report.model.request.BusinessTodayRequest;
import com.nemi.report.model.response.BusinessTodayResponse;
import com.nemi.report.service.BusinessTodayService;
import org.springframework.stereotype.Service;

@Service
public class BusinessTodayServiceImpl implements BusinessTodayService {
    @Override
    public BusinessTodayResponse getBusinessToday(BusinessTodayRequest request) {
        return null;
    }
}
