package com.nemi.report.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.report.constant.ConfirmOrderWhen;
import lombok.Data;

@Data
public class ConfigResponse {

    @JsonProperty("confirmOrderWhen")
    private ConfirmOrderWhen confirmOrderWhen;

    @JsonProperty("returnOrderWhen")
    private ConfirmOrderWhen returnOrderWhen;
}
