package com.nemi.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "monthly_target", schema = "system_manager")
@IdClass(MonthlyTargetId.class)
public class MonthlyTargetEntity extends BaseEntity {
    @Id
    @Column(name = "department_id")
    private String departmentId;

    @Id
    @Column(name = "currency")
    private String currency;

    @Column(name = "revenue")
    private BigDecimal revenue;

    @Column(name = "returned_order_percent")
    private BigDecimal returnedOrderPercent;

    @Column(name = "ad_cost_per_revenue")
    private BigDecimal adCostPerRevenue;

    @Column(name = "company_id")
    private Integer companyId;
}
