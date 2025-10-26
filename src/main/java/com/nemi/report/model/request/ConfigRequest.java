package com.nemi.report.model.request;

import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.ReturnOrderWhen;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigRequest {

    @NotNull
    private ConfirmOrderWhen confirmOrderWhen;

    @NotNull
    private ReturnOrderWhen returnOrderWhen;
}
