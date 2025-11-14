package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.ReturnOrderWhen;
import com.nemi.report.entity.ReportSettingEntity;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.model.request.overview.UpdateReportSettingRequest;
import com.nemi.report.model.response.overview.ReportSettingResponse;
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
    public ReportSettingResponse getConfig() {
        String departmentId = claimUtil.getDepartmentId();
        log.debug("[ConfigServiceImpl.getConfig] Fetching config for departmentId={}", departmentId);
        return reportSettingRepository.findById(departmentId)
                .map(entity -> {
                    log.debug("[ConfigServiceImpl.getConfig] Loaded config: confirmOrderWhen={}, returnOrderWhen={}",
                            entity.getConfirmOrderWhen(), entity.getReturnOrderWhen());

                    return ReportSettingResponse.builder()
                            .confirmOrderWhen(ConfirmOrderWhen.fromCode(entity.getConfirmOrderWhen()))
                            .returnOrderWhen(ReturnOrderWhen.fromCode(entity.getReturnOrderWhen()))
                            .build();
                })
                .orElseGet(() -> ReportSettingResponse.builder()
                        .confirmOrderWhen(ConfirmOrderWhen.UPDATE_STATUS_TO_NEW)
                        .returnOrderWhen(ReturnOrderWhen.RETURNED)
                        .build());
    }

    @Override
    public ReportSettingResponse updateConfig(UpdateReportSettingRequest request) {
        String username = claimUtil.getUserName();
        String departmentId = claimUtil.getDepartmentId();

        log.trace("[ConfigServiceImpl.updateConfig] Updating config for departmentId={}, Request payload: confirmOrderWhen={}, returnOrderWhen={}",
                departmentId, request.getConfirmOrderWhen(), request.getReturnOrderWhen());

        try {
            Optional<ReportSettingEntity> reportSettingEntity = reportSettingRepository.findById(departmentId);

            ReportSettingEntity reportSetting;
            if (reportSettingEntity.isPresent()) {
                reportSetting = reportSettingEntity.get();
                reportSetting.setUpdatedAt(LocalDateTime.now());
                reportSetting.setUpdatedBy(username);
            } else {
                reportSetting = ReportSettingEntity.builder()
                        .departmentId(departmentId)
                        .companyId(claimUtil.getCompanyId())
                        .createdBy(username)
                        .build();
            }

            reportSetting.setConfirmOrderWhen(request.getConfirmOrderWhen().getCode());
            reportSetting.setReturnOrderWhen(request.getReturnOrderWhen().getCode());
            reportSettingRepository.save(reportSetting);

            log.info("[ConfigServiceImpl.updateConfig] Config updated successfully for departmentId={}", departmentId);

            return ReportSettingResponse.builder()
                    .confirmOrderWhen(request.getConfirmOrderWhen())
                    .returnOrderWhen(request.getReturnOrderWhen())
                    .build();
        } catch (Exception e) {
            log.error("[ConfigServiceImpl.updateConfig] Failed to update config for departmentId={} - error={}", departmentId, e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.CONFIG_UPDATE_ERROR));
        }
    }
}
