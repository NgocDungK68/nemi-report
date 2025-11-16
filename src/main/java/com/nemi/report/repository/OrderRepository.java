package com.nemi.report.repository;

import com.nemi.report.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Lọc theo nhiều status + khoảng thời gian
//    @Query("SELECT o FROM OrderEntity o WHERE o.status IN :statuses AND o.updatedAt BETWEEN :from AND :to")
    List<OrderEntity> findByDepartmentIdAndStatusInAndUpdatedAtBetween(@Param("departmentId") String departmentId,
                                                                       @Param("statuses") List<String> statuses,
                                                                       @Param("from") LocalDateTime from,
                                                                       @Param("to") LocalDateTime to);
}
