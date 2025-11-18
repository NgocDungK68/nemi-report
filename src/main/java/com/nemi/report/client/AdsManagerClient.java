package com.nemi.report.client;

import com.nemi.config.feign.PeerServiceFeignClientConfig;
import com.nemi.report.model.ads_manager.AdsCostOfDepartmentRequest;
import com.nemi.report.model.ads_manager.AdsCostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "ads-manager-client",
        url = "${nemi.feign.ads-manager.base-url:http://nemi-ads-manager:8080}",
        configuration = {PeerServiceFeignClientConfig.class}
)
public interface AdsManagerClient {

    @PostMapping("service-api/v1/ad-cost-of-department")
    ResponseEntity<AdsCostResponse> getAdsCostOfDepartment(@RequestBody AdsCostOfDepartmentRequest request);
}
