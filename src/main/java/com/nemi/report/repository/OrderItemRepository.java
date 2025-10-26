package com.nemi.report.repository;

import com.nemi.report.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {
    @Query("SELECT SUM(i.quantity) FROM OrderItemEntity i WHERE i.orderId IN :orderIds")
    BigDecimal sumQuantityByOrderIds(@Param("orderIds") List<String> orderIds);
}
