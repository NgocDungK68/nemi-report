package com.nemi.report.service.impl;

import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.ReturnOrderWhen;
import com.nemi.report.entity.ReportSettingEntity;
import com.nemi.report.model.request.ConfigRequest;
import com.nemi.report.model.response.ConfigResponse;
import com.nemi.report.repository.ReportSettingRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {
    private final ClaimUtil claimUtil;
    private final ReportSettingRepository reportSettingRepository;

    @Override
    public ConfigResponse getConfig() {
        return reportSettingRepository.findById(claimUtil.getDepartmentId())
                .map(entity -> ConfigResponse.builder()
                        .confirmOrderWhen(ConfirmOrderWhen.fromCode(entity.getConfirmOrderWhen()))
                        .returnOrderWhen(ReturnOrderWhen.fromCode(entity.getReturnOrderWhen()))
                        .build()
                )
                .orElseGet(() -> ConfigResponse.builder()
                        .confirmOrderWhen(ConfirmOrderWhen.UPDATE_STATUS_TO_NEW)
                        .returnOrderWhen(ReturnOrderWhen.RETURNED)
                        .build()
                );
    }

    @Override
    public ConfigResponse updateConfig(ConfigRequest request) {
        Optional<ReportSettingEntity> reportSettingEntity = reportSettingRepository.findById(claimUtil.getDepartmentId());

        ReportSettingEntity reportSetting = reportSettingEntity.orElseGet(() -> ReportSettingEntity.builder()
                .departmentId(claimUtil.getDepartmentId())
                .companyId(claimUtil.getCompanyId())
                .updatedBy(claimUtil.getUserName())
                .build());
        reportSetting.setConfirmOrderWhen(request.getConfirmOrderWhen().getCode());
        reportSetting.setReturnOrderWhen(request.getReturnOrderWhen().getCode());
        reportSetting.setUpdatedTime(LocalDateTime.now());

        // TODO: call lại toàn bộ hàm trong overviewReport?

        return ConfigResponse.builder()
                .confirmOrderWhen(request.getConfirmOrderWhen())
                .returnOrderWhen(request.getReturnOrderWhen())
                .build();
    }
}
