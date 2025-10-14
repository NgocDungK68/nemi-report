package com.nemi.report.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

@RestController
public class TestController {

    @GetMapping("/service-api/test1")
    public String testService() {
        return "Test service api successful-user manager";
    }
}
