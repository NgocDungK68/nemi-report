package com.nemi.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "report_setting", schema = "system_manager")
public class ReportSettingEntity extends BaseEntity {

    @Id
    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "confirm_order_when")
    private String confirmOrderWhen;

    @Column(name = "return_order_when")
    private String returnOrderWhen;
}
