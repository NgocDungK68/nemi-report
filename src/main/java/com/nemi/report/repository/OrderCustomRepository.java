package com.nemi.report.repository;

import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.constant.ReturnOrderWhen;
import com.nemi.report.model.pojo.OrderQueryModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCustomRepository {

    private final EntityManager em;

    public List<OrderQueryModel> reportOrderOfDepartmentByDate(String departmentId,
                                                               LocalDate startDate, LocalDate endDate,
                                                               ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !StringUtils.equals(s, OrderStatus.RETURNED.name())).toList();

        String queryString = """
                SELECT
                    s.order_date AS report_date,
                    SUM(s.total) AS orders,
                    SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END) AS confirmed_orders,
                    SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END) AS returned_orders,
                    SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END) AS success_orders,
                    SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                    SUM(CASE WHEN s.status in :confirmed_status_without_returned THEN s.revenue - coalesce(s.discount, 0) ELSE 0 END) AS true_revenue
                FROM (
                    SELECT
                        TO_CHAR(o.created_at, 'DD/MM/YYYY') AS order_date,
                        o.status,
                        COUNT(1) AS total,
                        SUM(o.total_price) AS revenue,
                        SUM(o.discount_amount) AS discount
                    FROM product_manager.orders o
                    WHERE o.department_id = :department_id
                        AND o.created_at >= :start_date
                        AND o.created_at < :end_date
                    GROUP BY TO_CHAR(o.created_at, 'DD/MM/YYYY'), o.status
                ) AS s
                GROUP BY s.order_date
                ORDER BY s.order_date;
                """;

        Query query = em.createNativeQuery(queryString);
        query.setParameter("department_id", departmentId);
        query.setParameter("confirmed_status", confirmedStatus);
        query.setParameter("returned_status", returnedStatus);
        query.setParameter("success_status", OrderStatus.getSuccessfulOrdersStatus());
        query.setParameter("confirmed_status_without_returned", confirmedStatusWithoutReturned);
        query.setParameter("start_date", startDate.atStartOfDay());
        query.setParameter("end_date", endDate.plusDays(1).atStartOfDay());

        log.debug(query.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new OrderQueryModel(
                        (String) row[0],                    // date
                        ((Number) row[1]).longValue(),      // orders
                        ((Number) row[2]).longValue(),      // confirmed_orders
                        ((Number) row[3]).longValue(),      // returned_orders
                        ((Number) row[4]).longValue(),      // success_orders
                        (BigDecimal) row[5],                // revenue
                        (BigDecimal) row[6]                 // true_revenue
                ))
                .toList();
    }
}
