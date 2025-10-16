package com.nemi.report.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ConfigResponse {

    @JsonProperty("confirmOrderWhen")
    private List<String> confirmOrderWhen;

    @JsonProperty("returnOrderWhen")
    private List<String> returnOrderWhen;
}
