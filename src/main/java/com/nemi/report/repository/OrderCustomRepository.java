package com.nemi.report.repository;

import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.constant.ReturnOrderWhen;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByProductModel;
import com.nemi.report.model.pojo.OrderQueryByUserModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.util.QueryResolver;
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

    private static final String TEMP_TABLE = "data";

    public List<OrderQueryByDateModel> reportOrderByDate(String departmentId,
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
                         left join product_manager.pos p on o.pos_id = p.id
                    WHERE p.department_id = :department_id
                        and p.status <> ('ACTIVE', 'PROCESSING')
                        AND o.created_at >= :start_date
                        AND o.created_at < :end_date
                    GROUP BY TO_CHAR(o.created_at, 'DD/MM/YYYY'), o.status
                ) AS s
                GROUP BY s.order_date
                ORDER BY s.order_date;
                """;

        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> OrderQueryByDateModel.builder()
                        .reportDate((String) row[0])
                        .orders(((Number) row[1]).longValue())
                        .confirmedOrders(((Number) row[2]).longValue())
                        .returnedOrders(((Number) row[3]).longValue())
                        .successOrders(((Number) row[4]).longValue())
                        .revenue((BigDecimal) row[5])
                        .trueRevenue((BigDecimal) row[6])
                        .build())
                .toList();
    }

    private Query createNativeQuery(String departmentId, LocalDate startDate, LocalDate endDate, List<String> confirmedStatus, List<String> returnedStatus, List<String> confirmedStatusWithoutReturned, String queryString) {
        Query query = em.createNativeQuery(queryString);
        query.setParameter("department_id", departmentId);
        query.setParameter("confirmed_status", confirmedStatus);
        query.setParameter("returned_status", returnedStatus);
        query.setParameter("success_status", OrderStatus.getSuccessfulOrdersStatus());
        query.setParameter("confirmed_status_without_returned", confirmedStatusWithoutReturned);
        query.setParameter("start_date", startDate.atStartOfDay());
        query.setParameter("end_date", endDate.plusDays(1).atStartOfDay());

        return query;
    }

    public List<OrderQueryByUserModel> reportOrderByUser(String departmentId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         List<ColumnRequest> orders, List<FilterRequest> filters,
                                                         ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !StringUtils.equals(s, OrderStatus.RETURNED.name())).toList();

        StringBuilder queryString = new StringBuilder("""
                select u.id as user_id,
                       u.name,
                       u.image,
                       coalesce(data.orders, 0) as orders,
                       coalesce(data.confirmed_orders, 0) as confirmed_orders,
                       coalesce(data.returned_orders, 0) as returned_orders,
                       coalesce(data.success_orders, 0) as success_orders,
                       coalesce(data.revenue, 0) as revenue,
                       coalesce(data.true_revenue, 0) as true_revenue
                from user_manager.user u
                         inner join user_manager.department_user du
                                   on u.id = du.user_id and du.department_id = :department_id and du.status = 'ACTIVE'
                         left join (select upl.user_id                                                            AS user_id,
                                           SUM(s.total)                                                           AS orders,
                                           SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)   AS confirmed_orders,
                                           SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)    AS returned_orders,
                                           SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END)     AS success_orders,
                                           SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                                           SUM(CASE
                                                   WHEN s.status in :confirmed_status_without_returned
                                                       THEN s.revenue - coalesce(s.discount, 0)
                                                   ELSE 0 END)                                                    AS true_revenue
                                    from (select o.sale_id              as sale_id,
                                                 o.status,
                                                 COUNT(1)               AS total,
                                                 SUM(o.total_price)     AS revenue,
                                                 SUM(o.discount_amount) AS discount
                                          from product_manager.orders o
                                                   left join product_manager.pos p on o.pos_id = p.id
                                          where p.department_id = :department_id
                                            and p.status in ('ACTIVE', 'PROCESSING')
                                            and o.created_at >= :start_date
                                            and o.created_at < :end_date
                                          group by o.sale_id, o.status) s
                                             left join user_manager.user_pos_link upl on s.sale_id = upl.pos_user_id
                                    group by upl.user_id) data on data.user_id = u.id
                where 1 = 1
                """);

        // append filter if present
        if (!filters.isEmpty()) {
            queryString.append(" AND ");
            queryString.append(QueryResolver.toWhereClause(TEMP_TABLE, filters));
        }

        // append order if present
        if (!orders.isEmpty()) {
            queryString.append(" order by ");
            queryString.append(QueryResolver.toOrderClause(TEMP_TABLE, orders));
        }

        // create query and pagination
        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> OrderQueryByUserModel.builder()
                        .userId((String) row[0])
                        .name((String) row[1])
                        .image((String) row[2])
                        .orders(((Number) row[3]).longValue())
                        .confirmedOrders(((Number) row[4]).longValue())
                        .returnedOrders(((Number) row[5]).longValue())
                        .successOrders(((Number) row[6]).longValue())
                        .revenue((BigDecimal) row[7])
                        .trueRevenue((BigDecimal) row[8])
                        .build())
                .toList();
    }

    public List<OrderQueryByDateModel> reportOrderByDateOfUser(String departmentId, String userId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !StringUtils.equals(s, OrderStatus.RETURNED.name())).toList();

        String queryString = """
                select upl.user_id                                                            AS user_id,
                       s.order_date                                                           AS order_date,
                       SUM(s.total)                                                           AS orders,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)   AS confirmed_orders,
                       SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)    AS returned_orders,
                       SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END)     AS success_orders,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                       SUM(CASE
                               WHEN s.status in :confirmed_status_without_returned
                                   THEN s.revenue - coalesce(s.discount, 0)
                               ELSE 0 END)                                                    AS true_revenue
                from (select o.sale_id                           as sale_id,
                             TO_CHAR(o.created_at, 'DD/MM/YYYY') AS order_date,
                             o.status,
                             COUNT(1)                            AS total,
                             SUM(o.total_price)                  AS revenue,
                             SUM(o.discount_amount)              AS discount
                      from product_manager.orders o
                               left join product_manager.pos p on o.pos_id = p.id
                      where p.department_id = :department_id
                        and p.status in ('ACTIVE', 'PROCESSING')
                        and o.created_at >= :start_date
                        and o.created_at < :end_date
                      group by o.sale_id, TO_CHAR(o.created_at, 'DD/MM/YYYY'), o.status) s
                         left join user_manager.user_pos_link upl on s.sale_id = upl.pos_user_id
                where upl.user_id = :user_id
                group by upl.user_id, s.order_date
                order by s.order_date;
                """;

        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString);
        query.setParameter("user_id", userId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> OrderQueryByDateModel.builder()
                        .reportDate((String) row[1])
                        .orders(((Number) row[2]).longValue())
                        .confirmedOrders(((Number) row[3]).longValue())
                        .returnedOrders(((Number) row[4]).longValue())
                        .successOrders(((Number) row[5]).longValue())
                        .revenue((BigDecimal) row[6])
                        .trueRevenue((BigDecimal) row[7])
                        .build())
                .toList();
    }

    public List<OrderQueryByProductModel> reportOrderByProduct(String departmentId,
                                                               LocalDate startDate, LocalDate endDate,
                                                               List<ColumnRequest> orders, List<FilterRequest> filters,
                                                               ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !StringUtils.equals(s, OrderStatus.RETURNED.name())).toList();

        StringBuilder queryString = new StringBuilder("""
                SELECT p.product_id AS product_id,
                       p.name,
                       p.images     AS image,
                       COUNT(o.order_id)                                                                         AS orders,
                       SUM(CASE WHEN o.status IN :confirmed_status THEN 1 ELSE 0 END)                            AS confirmed_orders,
                       SUM(CASE WHEN o.status IN :returned_status THEN 1 ELSE 0 END)                             AS returned_orders,
                       SUM(CASE WHEN o.status IN :success_status THEN 1 ELSE 0 END)                              AS success_orders,
                       SUM(CASE WHEN o.status IN :confirmed_status THEN COALESCE(oi.total_price, 0) ELSE 0 END)  AS revenue,
                       SUM(CASE WHEN o.status in :confirmed_status_without_returned
                                   THEN COALESCE(oi.total_price, 0)
                               ELSE 0 END)                                                                       AS true_revenue
                FROM product_manager.products p
                         LEFT JOIN product_manager.order_item oi
                                   ON oi.pos_id = p.pos_id AND oi.product_id = p.product_id
                         LEFT JOIN product_manager.orders o
                                   ON o.order_id = oi.order_id
                                  AND o.department_id = :department_id
                                  AND o.created_at >= :start_date
                                  AND o.created_at < :end_date
                         LEFT JOIN product_manager.pos pos
                                   ON pos.id = o.pos_id
                                  AND pos.status IN ('ACTIVE', 'PROCESSING')
                WHERE p.department_id = :department_id
                """);

        // append filter if present
        if (!filters.isEmpty()) {
            queryString.append(" AND ");
            queryString.append(QueryResolver.toWhereClause(TEMP_TABLE, filters));
        }

        // append order if present
        if (!orders.isEmpty()) {
            queryString.append(" ORDER BY ");
            queryString.append(QueryResolver.toOrderClause(TEMP_TABLE, orders));
        }

        queryString.append("GROUP BY p.product_id, p.name, p.image");

        // create query and pagination
        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> OrderQueryByProductModel.builder()
                        .productId((String) row[0])
                        .name((String) row[1])
                        .image((String) row[2])
                        .orders(((Number) row[3]).longValue())
                        .confirmedOrders(((Number) row[4]).longValue())
                        .returnedOrders(((Number) row[5]).longValue())
                        .successOrders(((Number) row[6]).longValue())
                        .revenue((BigDecimal) row[7])
                        .trueRevenue((BigDecimal) row[8])
                        .build())
                .toList();
    }

    public List<OrderQueryByDateModel> reportOrderByDateOfProduct(String departmentId, String productId,
                                                               LocalDate startDate, LocalDate endDate,
                                                               ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !StringUtils.equals(s, OrderStatus.RETURNED.name())).toList();

        String queryString = """
                SELECT p.product_id                         AS product_id,
                       TO_CHAR(o.created_at, 'DD/MM/YYYY')  AS order_date,
                       COUNT(o.order_id)                                                                         AS orders,
                       SUM(CASE WHEN o.status IN :confirmed_status THEN 1 ELSE 0 END)                            AS confirmed_orders,
                       SUM(CASE WHEN o.status IN :returned_status THEN 1 ELSE 0 END)                             AS returned_orders,
                       SUM(CASE WHEN o.status IN :success_status THEN 1 ELSE 0 END)                              AS success_orders,
                       SUM(CASE WHEN o.status IN :confirmed_status THEN COALESCE(oi.total_price, 0) ELSE 0 END)  AS revenue,
                       SUM(CASE WHEN o.status in :confirmed_status_without_returned
                                   THEN COALESCE(oi.total_price, 0)
                               ELSE 0 END)                                                                       AS true_revenue
                FROM product_manager.products p
                         LEFT JOIN product_manager.order_item oi
                                   ON oi.pos_id = p.pos_id AND oi.product_id = p.product_id
                         LEFT JOIN product_manager.orders o
                                   ON o.order_id = oi.order_id
                                  AND o.department_id = :department_id
                                  AND o.created_at >= :start_date
                                  AND o.created_at < :end_date
                         LEFT JOIN product_manager.pos pos
                                   ON pos.id = o.pos_id
                                  AND pos.status IN ('ACTIVE', 'PROCESSING')
                WHERE p.department_id = :department_id AND p.product_id = :product_id
                GROUP BY p.product_id, TO_CHAR(o.created_at, 'DD/MM/YYYY')
                ORDER BY o.created_at;
                """;

        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString);
        query.setParameter("product_id", productId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> OrderQueryByDateModel.builder()
                        .reportDate((String) row[1])
                        .orders(((Number) row[2]).longValue())
                        .confirmedOrders(((Number) row[3]).longValue())
                        .returnedOrders(((Number) row[4]).longValue())
                        .successOrders(((Number) row[5]).longValue())
                        .revenue((BigDecimal) row[6])
                        .trueRevenue((BigDecimal) row[7])
                        .build())
                .toList();
    }
}
