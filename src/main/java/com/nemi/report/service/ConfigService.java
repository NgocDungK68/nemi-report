package com.nemi.report.service;

import com.nemi.report.model.request.overview.UpdateReportSettingRequest;
import com.nemi.report.model.response.overview.ReportSettingResponse;

public interface ConfigService {
    ReportSettingResponse getConfig();
    ReportSettingResponse updateConfig(UpdateReportSettingRequest request);
}
