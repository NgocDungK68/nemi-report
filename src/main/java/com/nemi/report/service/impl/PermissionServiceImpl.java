package com.nemi.report.service.impl;

import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.CheckPermissionRequest;
import com.nemi.report.constant.ReportConstants;
import com.nemi.report.exception.TechnicalAlertCode;
import com.nemi.report.service.PermissionService;
import com.nemi.user_manager.model.pojo.Permission;
import com.nemi.user_manager.model.response.PermissionDetailResponse;
import com.nemi.user_manager.service_client.AccessControlClient;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final AccessControlClient accessControlClient;
    private final ClaimUtil claimUtil;

    @Override
    public List<Permission.CustomData> getAllowedData(String reportType) {
        CheckPermissionRequest request = new CheckPermissionRequest();
        request.setCompanyId(claimUtil.getCompanyId());
        request.setDepartmentId(claimUtil.getDepartmentId());
        request.setUserId(claimUtil.getUserId());
        request.setResource(ReportConstants.REPORT_RESOURCE);
        request.setFunction(reportType);

        ResponseEntity<PermissionDetailResponse> response = accessControlClient.getPermissionDetail(request);

        if (Objects.isNull(response.getBody())) {
            log.error("[getAllowedData] error call access control ");
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR));
        }

        return response.getBody().getCustomData();
    }
}
