package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.ReturnOrderWhen;
import com.nemi.report.entity.ReportSettingEntity;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.ConfigRequest;
import com.nemi.report.model.response.ConfigResponse;
import com.nemi.report.repository.ReportSettingRepository;
import com.nemi.report.service.ConfigService;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigServiceImpl implements ConfigService {
    private final ClaimUtil claimUtil;
    private final ReportSettingRepository reportSettingRepository;

    @Override
    public ConfigResponse getConfig() {
        String userId = claimUtil.getUserId();
        log.debug("[ConfigServiceImpl.getConfig] Fetching config for userId={}", userId);
        return reportSettingRepository.findById(userId)
                .map(entity -> {
                    log.debug("[ConfigServiceImpl.getConfig] Loaded config: confirmOrderWhen={}, returnOrderWhen={}",
                            entity.getConfirmOrderWhen(), entity.getReturnOrderWhen());

                    return ConfigResponse.builder()
                            .confirmOrderWhen(ConfirmOrderWhen.fromCode(entity.getConfirmOrderWhen()))
                            .returnOrderWhen(ReturnOrderWhen.fromCode(entity.getReturnOrderWhen()))
                            .build();
                })
                .orElseGet(() -> {
                    log.info("[ConfigServiceImpl.getConfig] No existing config found for userId={}, using defaults", userId);

                    return ConfigResponse.builder()
                            .confirmOrderWhen(ConfirmOrderWhen.UPDATE_STATUS_TO_NEW)
                            .returnOrderWhen(ReturnOrderWhen.RETURNED)
                            .build();
                });
    }

    @Override
    public ConfigResponse updateConfig(ConfigRequest request) {
        String userId = claimUtil.getUserId();

        log.trace("[ConfigServiceImpl.updateConfig] Updating config for userId={}, Request payload: confirmOrderWhen={}, returnOrderWhen={}",
                userId, request.getConfirmOrderWhen(), request.getReturnOrderWhen());

        try {
            Optional<ReportSettingEntity> reportSettingEntity = reportSettingRepository.findById(claimUtil.getUserId());

            ReportSettingEntity reportSetting = reportSettingEntity.orElseGet(() -> ReportSettingEntity.builder()
                    .userId(userId)
                    .departmentId(claimUtil.getDepartmentId())
                    .companyId(claimUtil.getCompanyId())
                    .updatedBy(claimUtil.getUserName())
                    .build());
            reportSetting.setConfirmOrderWhen(request.getConfirmOrderWhen().getCode());
            reportSetting.setReturnOrderWhen(request.getReturnOrderWhen().getCode());
            reportSetting.setUpdatedTime(LocalDateTime.now());
            reportSettingRepository.save(reportSetting);

            log.info("[ConfigServiceImpl.updateConfig] Config updated successfully for userId={}", userId);

            return ConfigResponse.builder()
                    .confirmOrderWhen(request.getConfirmOrderWhen())
                    .returnOrderWhen(request.getReturnOrderWhen())
                    .build();
        } catch (Exception e) {
            log.error("[ConfigServiceImpl.updateConfig] Failed to update config for userId={} - error={}", userId, e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.CONFIG_UPDATE_ERROR));
        }
    }
}
