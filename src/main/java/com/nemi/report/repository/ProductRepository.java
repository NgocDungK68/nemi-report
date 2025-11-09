package com.nemi.report.repository;

import com.nemi.report.entity.ProductEntity;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity,String > {

    @Query("""
    SELECT p FROM ProductEntity p 
    WHERE 
    ((:startDate IS NULL OR :endDate IS NULL)
        OR p.createdAt < :startDate 
        OR p.createdAt > :endDate)
""")
    Page<ProductEntity> findByCreatedAtOutsideRangeOptional(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
    SELECT p FROM ProductEntity p 
    WHERE 
    (:startDate IS NULL OR p.createdAt >= :startDate)
    AND 
    (:endDate IS NULL OR p.createdAt <= :endDate)
""")
    Page<ProductEntity> findByCreatedAtBetweenOptional(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

//    Page<ProductEntity> getAllData(Pageable pageable);

}
