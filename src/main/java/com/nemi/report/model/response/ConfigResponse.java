package com.nemi.report.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.report.constant.ConfirmOrderWhen;
import lombok.Data;

import java.util.List;

@Data
public class ConfigResponse {

    @JsonProperty("confirmOrderWhen")
    private ConfirmOrderWhen confirmOrderWhen;

    @JsonProperty("returnOrderWhen")
    private ConfirmOrderWhen returnOrderWhen;
}
