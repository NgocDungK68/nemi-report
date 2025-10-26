package com.nemi.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "report_setting", schema = "system_manager")
public class ReportSettingEntity {
    @Id
    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "confirm_order_when")
    private String confirmOrderWhen;

    @Column(name = "return_order_when")
    private String returnOrderWhen;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @Column(name = "updated_by")
    private String updatedBy;
}
