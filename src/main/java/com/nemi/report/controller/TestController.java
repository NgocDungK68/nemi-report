package com.nemi.report.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/service-api/test1")
    public String testService() {
        return "Test service api successful-user manager";
    }

    @GetMapping("/public-api/test2")
    public String testService2() {
        return "api test to update image";
    }
}
