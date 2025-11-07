package com.nemi.report.service;

import com.nemi.user_manager.model.pojo.Permission;

import java.util.List;

public interface PermissionService {
    List<Permission.CustomData> getAllowedData(String reportType);
}
