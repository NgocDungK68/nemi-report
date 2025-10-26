package com.nemi.report.repository;

import com.nemi.report.entity.ReportSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportSettingRepository extends JpaRepository<ReportSettingEntity, String> {
}
