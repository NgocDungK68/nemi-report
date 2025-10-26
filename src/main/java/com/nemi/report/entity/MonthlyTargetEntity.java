package com.nemi.report.entity;

import com.nemi.util.ClaimUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "monthly_target", schema = "system_manager")
public class MonthlyTargetEntity {
    @Id
    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "revenue")
    private BigDecimal revenue;

    @Column(name = "currency")
    private String currency;

    @Column(name = "returned_order_percent")
    private BigDecimal returnedOrderPercent;

    @Column(name = "ad_cost_per_revenue")
    private BigDecimal adCostPerRevenue;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @Column(name = "updated_by")
    private String updatedBy;
}
