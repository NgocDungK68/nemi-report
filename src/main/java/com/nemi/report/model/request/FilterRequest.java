package com.nemi.report.model.request;

import com.nemi.report.constant.FilterType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashSet;

@Data
public class FilterRequest {
    @NotNull
    private String code;
    private FilterType type;
    private LinkedHashSet<String> value = new LinkedHashSet<>();
}
