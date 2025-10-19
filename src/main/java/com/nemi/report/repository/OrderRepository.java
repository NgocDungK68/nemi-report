package com.nemi.report.repository;

import com.nemi.report.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Lấy tất cả orders trong khoảng thời gian
    List<OrderEntity> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Lọc theo nhiều status + khoảng thời gian
    List<OrderEntity> findByStatusInAndCreatedAtBetween(List<String> statuses, LocalDateTime from, LocalDateTime to);
}
