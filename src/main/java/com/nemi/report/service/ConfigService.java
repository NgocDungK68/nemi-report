package com.nemi.report.service;

import com.nemi.report.model.request.ConfigRequest;
import com.nemi.report.model.response.ConfigResponse;

public interface ConfigService {
    ConfigResponse getConfig();
    ConfigResponse updateConfig(ConfigRequest request);
}
