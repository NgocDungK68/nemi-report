package com.nemi.report.repository;

import com.nemi.report.entity.MonthlyTargetEntity;
import com.nemi.report.entity.MonthlyTargetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyTargetRepository extends JpaRepository<MonthlyTargetEntity, MonthlyTargetId> {
}
