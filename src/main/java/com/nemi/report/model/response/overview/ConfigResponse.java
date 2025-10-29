package com.nemi.report.model.response.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.ReturnOrderWhen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigResponse {

    @JsonProperty("confirmOrderWhen")
    private ConfirmOrderWhen confirmOrderWhen;

    @JsonProperty("returnOrderWhen")
    private ReturnOrderWhen returnOrderWhen;
}
