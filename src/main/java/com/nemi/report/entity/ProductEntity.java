package com.nemi.report.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.nemi.report.entity.key.OrderPosId;
import com.nemi.report.entity.key.ProductPosId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
@Table(name = "products", schema = "product_manager")
@IdClass(ProductPosId.class)
public class ProductEntity extends BaseEntity {
    @Id
    @Column(name = "product_id")
    private String productId;

    @Id
    @Column(name = "pos_id")
    private String posId;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "brand")
    private String brand;

    @Column(name = "category")
    private String category;

    @Column(name = "status")
    private String status;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @JsonProperty("department_id")
    private String departmentId;
}
