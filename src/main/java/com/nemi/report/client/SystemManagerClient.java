package com.nemi.report.client;

import com.nemi.config.feign.PeerServiceFeignClientConfig;
import com.nemi.report.model.system_manager.ExchangeRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "system-manager-client",
        url = "${nemi.feign.system-manager.base-url:http://nemi-system-manager:8080}",
        configuration = {PeerServiceFeignClientConfig.class}
)
public interface SystemManagerClient {

    @GetMapping("/service-api/v1/exchange-rate")
    ResponseEntity<ExchangeRateResponse> getExchangeRate(@RequestParam("company-id") Integer companyId,
                                                         @RequestParam("currency-from") String currencyFrom,
                                                         @RequestParam("currency-to") String currencyTo,
                                                         @RequestParam("date-from") String dateFrom,
                                                         @RequestParam("date-to") String dateTo);
}
