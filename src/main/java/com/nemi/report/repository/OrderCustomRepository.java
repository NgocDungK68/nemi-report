package com.nemi.report.repository;

import com.nemi.report.constant.ConfirmOrderWhen;
import com.nemi.report.constant.OrderStatus;
import com.nemi.report.constant.ReturnOrderWhen;
import com.nemi.report.model.pojo.FullOrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByDateModel;
import com.nemi.report.model.pojo.OrderQueryByHourModel;
import com.nemi.report.model.pojo.OrderQueryByProductModel;
import com.nemi.report.model.pojo.OrderQueryByUserModel;
import com.nemi.report.model.pojo.OrderSumModel;
import com.nemi.report.model.request.ColumnRequest;
import com.nemi.report.model.request.FilterRequest;
import com.nemi.report.util.QueryResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    public List<FullOrderQueryByDateModel> fullReportOrderByDate(String departmentId,
                                                                 LocalDate startDate, LocalDate endDate,
                                                                 ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

        String queryString = """
                SELECT s.order_date                                                            AS report_date,
                       SUM(s.total)                                                            AS orders,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)    AS confirmed_orders,
                       SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)     AS returned_orders,
                       SUM(CASE WHEN s.status in :delivering_status THEN s.total ELSE 0 END)   AS delivering_orders,
                       SUM(CASE WHEN s.status in :pending_status THEN s.total ELSE 0 END)      AS pending_orders,
                       SUM(CASE WHEN s.status in :canceled_status THEN s.total ELSE 0 END)      AS canceled_orders,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END)  AS revenue,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END)  AS confirmed_revenue,
                       SUM(CASE WHEN s.status in :returned_status THEN s.revenue ELSE 0 END)   AS returned_revenue,
                       SUM(CASE WHEN s.status in :delivering_status THEN s.revenue ELSE 0 END) AS delivering_revenue,
                       SUM(CASE WHEN s.status in :pending_status THEN s.revenue ELSE 0 END)    AS pending_revenue,
                       SUM(CASE WHEN s.status in :canceled_status THEN s.revenue ELSE 0 END)    AS canceled_revenue,
                       SUM(CASE
                               WHEN s.status in :confirmed_status_without_returned THEN s.revenue - coalesce(s.discount, 0)
                               ELSE 0 END)                                                     AS true_revenue
                FROM (SELECT TO_CHAR(o.created_at, 'DD/MM/YYYY') AS order_date,
                             o.status,
                             COUNT(1)                            AS total,
                             SUM(o.total_price)                  AS revenue,
                             SUM(o.discount_amount)              AS discount
                      FROM product_manager.orders o
                               left join product_manager.pos p on o.pos_id = p.id
                      WHERE p.department_id = :department_id
                        and p.status in ('ACTIVE', 'PROCESSING')
                        AND o.created_at >= :start_date
                        AND o.created_at < :end_date
                      GROUP BY TO_CHAR(o.created_at, 'DD/MM/YYYY'), o.status) AS s
                GROUP BY s.order_date
                """;

        Query query = createNativeQueryFullOrder(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> FullOrderQueryByDateModel.builder()
                        .reportDate((String) row[0])
                        .orders(((Number) row[1]).longValue())
                        .confirmedOrders(((Number) row[2]).longValue())
                        .returnedOrders(((Number) row[3]).longValue())
                        .deliveringOrders(((Number) row[4]).longValue())
                        .pendingOrders(((Number) row[5]).longValue())
                        .canceledOrders(((Number) row[6]).longValue())
                        .revenue((BigDecimal) row[7])
                        .confirmedRevenue((BigDecimal) row[8])
                        .returnedRevenue((BigDecimal) row[9])
                        .deliveringRevenue((BigDecimal) row[10])
                        .pendingRevenue((BigDecimal) row[11])
                        .canceledRevenue((BigDecimal) row[12])
                        .trueRevenue((BigDecimal) row[13])
                        .build())
                .toList();
    }

    public List<OrderQueryByDateModel> reportOrderByDate(String departmentId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

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
                        and p.status in ('ACTIVE', 'PROCESSING')
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

    public List<OrderQueryByHourModel> reportOrderByHour(String departmentId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

        String queryString = """
                SELECT s.order_hour        AS report_hour,
                       SUM(s.total)        AS orders,
                       SUM(CASE
                               WHEN s.status in :confirmed_status_without_returned THEN s.revenue - coalesce(s.discount, 0)
                               ELSE 0 END) AS true_revenue
                FROM (SELECT TO_CHAR(o.created_at, 'HH24') AS order_hour,
                             o.status,
                             COUNT(1)                            AS total,
                             SUM(o.total_price)                  AS revenue,
                             SUM(o.discount_amount)              AS discount
                      FROM product_manager.orders o
                               left join product_manager.pos p on o.pos_id = p.id
                      WHERE p.department_id = :department_id
                        and p.status in ('ACTIVE', 'PROCESSING')
                        AND o.created_at >= :start_date
                        AND o.created_at < :end_date
                      GROUP BY TO_CHAR(o.created_at, 'HH24'), o.status) AS s
                GROUP BY s.order_hour order by s.order_hour
                """;

        Query query = em.createNativeQuery(queryString);
        query.setParameter("department_id", departmentId);
        query.setParameter("confirmed_status_without_returned", confirmedStatusWithoutReturned);
        query.setParameter("start_date", startDate.atStartOfDay());
        query.setParameter("end_date", endDate.plusDays(1).atStartOfDay());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> OrderQueryByHourModel.builder()
                        .reportHour((Integer) row[0])
                        .orders(((Number) row[1]).longValue())
                        .trueRevenue((BigDecimal) row[2])
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

    private Query createNativeQueryFullOrder(String departmentId, LocalDate startDate, LocalDate endDate, List<String> confirmedStatus, List<String> returnedStatus, List<String> confirmedStatusWithoutReturned, String queryString) {
        Query query = em.createNativeQuery(queryString);
        query.setParameter("department_id", departmentId);
        query.setParameter("confirmed_status", confirmedStatus);
        query.setParameter("returned_status", returnedStatus);
        query.setParameter("delivering_status", OrderStatus.getDeliveringOrdersStatus());
        query.setParameter("pending_status", OrderStatus.getPendingOrdersStatus());
        query.setParameter("canceled_status", OrderStatus.getCanceledOrdersStatus());
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
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

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
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

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

    public List<OrderQueryByProductModel> reportOrderByProduct(String departmentId, PageRequest pageRequest,
                                                               LocalDate startDate, LocalDate endDate,
                                                               List<ColumnRequest> orders, List<FilterRequest> filters,
                                                               ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

        StringBuilder queryString = new StringBuilder("""
                select *
                from (select product_id,
                             product_name,
                             product_images,
                             SUM(s.total)                                                           AS orders,
                             SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)   AS confirmed_orders,
                             SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)    AS returned_orders,
                             SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END)     AS success_orders,
                             SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                             SUM(CASE
                                     WHEN s.status in :confirmed_status_without_returned
                                         THEN s.revenue
                                     ELSE 0 END)                                                    AS true_revenue
                      from (select p.product_id        as product_id,
                                   p.name              as product_name,
                                   p.images            as product_images,
                                   o.status,
                                   COUNT(1)            AS total,
                                   SUM(oi.total_price) AS revenue
                            from product_manager.products p
                                     left join product_manager.pos pos on p.pos_id = pos.id
                                     left join product_manager.order_item oi on oi.product_id = p.product_id
                                     left join product_manager.orders o on oi.order_id = o.order_id
                            where pos.department_id = :department_id
                              and pos.status in ('ACTIVE', 'PROCESSING')
                              and o.created_at >= :start_date
                              and o.created_at < :end_date
                            group by p.product_id, p.name, p.images, o.status) s
                      group by s.product_id, s.product_name, s.product_images) data
                where 1 = 1
                """);

        // append filter if present
        if (!filters.isEmpty()) {
            queryString.append(" AND ");
            queryString.append(QueryResolver.toWhereClause(TEMP_TABLE, filters));
        }

        queryString.append(" group by data.product_id, data.product_name, data.product_images, data.orders, data.confirmed_orders, data.returned_orders, data.success_orders, data.revenue, data.true_revenue ");

        // append order if present
        if (!orders.isEmpty()) {
            queryString.append(" order by ");
            queryString.append(QueryResolver.toOrderClause(TEMP_TABLE, orders));
        }

        // create query and pagination
        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString.toString());
        query.setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize());
        query.setMaxResults(pageRequest.getPageSize());

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

    public Long reportCountOrderByProduct(String departmentId,
                                          LocalDate startDate, LocalDate endDate,
                                          List<ColumnRequest> orders, List<FilterRequest> filters,
                                          ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

        StringBuilder queryString = new StringBuilder("""
                select count(1) from (select *
                from (select product_id,
                             product_name,
                             product_images,
                             SUM(s.total)                                                           AS orders,
                             SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)   AS confirmed_orders,
                             SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)    AS returned_orders,
                             SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END)     AS success_orders,
                             SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                             SUM(CASE
                                     WHEN s.status in :confirmed_status_without_returned
                                         THEN s.revenue
                                     ELSE 0 END)                                                    AS true_revenue
                      from (select p.product_id        as product_id,
                                   p.name              as product_name,
                                   p.images            as product_images,
                                   o.status,
                                   COUNT(1)            AS total,
                                   SUM(oi.total_price) AS revenue
                            from product_manager.products p
                                     left join product_manager.pos pos on p.pos_id = pos.id
                                     left join product_manager.order_item oi on oi.product_id = p.product_id
                                     left join product_manager.orders o on oi.order_id = o.order_id
                            where pos.department_id = :department_id
                              and pos.status in ('ACTIVE', 'PROCESSING')
                              and o.created_at >= :start_date
                              and o.created_at < :end_date
                            group by p.product_id, p.name, p.images, o.status) s
                      group by s.product_id, s.product_name, s.product_images) data
                where 1 = 1
                """);

        // append filter if present
        if (!filters.isEmpty()) {
            queryString.append(" AND ");
            queryString.append(QueryResolver.toWhereClause(TEMP_TABLE, filters));
        }

        queryString.append(" group by data.product_id, data.product_name, data.product_images, data.orders, data.confirmed_orders, data.returned_orders, data.success_orders, data.revenue, data.true_revenue ");

        // append order if present
        if (!orders.isEmpty()) {
            queryString.append(" order by ");
            queryString.append(QueryResolver.toOrderClause(TEMP_TABLE, orders));
        }

        // end script
        queryString.append(" ) final;");

        // create query and pagination
        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString.toString());

        return (Long) query.getSingleResult();
    }

    public OrderSumModel reportSumOrderByProduct(String departmentId,
                                                 LocalDate startDate, LocalDate endDate,
                                                 List<ColumnRequest> orders, List<FilterRequest> filters,
                                                 ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

        StringBuilder queryString = new StringBuilder("""
                select SUM(final.orders)           as orders,
                       SUM(final.confirmed_orders) as confirmed_orders,
                       SUM(final.returned_orders)  as returned_orders,
                       SUM(final.success_orders)   as success_orders,
                       SUM(final.revenue)          as revenue,
                       SUM(final.true_revenue)     as true_revenue
                from (select *
                      from (select product_id,
                                   product_name,
                                   product_images,
                                   SUM(s.total)                                                           AS orders,
                                   SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)   AS confirmed_orders,
                                   SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)    AS returned_orders,
                                   SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END)     AS success_orders,
                                   SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                                   SUM(CASE
                                           WHEN s.status in :confirmed_status_without_returned
                                               THEN s.revenue
                                           ELSE 0 END)                                                    AS true_revenue
                            from (select p.product_id        as product_id,
                                         p.name              as product_name,
                                         p.images            as product_images,
                                         o.status,
                                         COUNT(1)            AS total,
                                         SUM(oi.total_price) AS revenue
                                  from product_manager.products p
                                           left join product_manager.pos pos on p.pos_id = pos.id
                                           left join product_manager.order_item oi on oi.product_id = p.product_id
                                           left join product_manager.orders o on oi.order_id = o.order_id
                                  where pos.department_id = :department_id
                                    and pos.status in ('ACTIVE', 'PROCESSING')
                                    and o.created_at >= :start_date
                                    and o.created_at < :end_date
                                  group by p.product_id, p.name, p.images, o.status) s
                            group by s.product_id, s.product_name, s.product_images) data
                      where 1 = 1
                """);

        // append filter if present
        if (!filters.isEmpty()) {
            queryString.append(" AND ");
            queryString.append(QueryResolver.toWhereClause(TEMP_TABLE, filters));
        }

        queryString.append(" group by data.product_id, data.product_name, data.product_images, data.orders, data.confirmed_orders, data.returned_orders, data.success_orders, data.revenue, data.true_revenue ");

        // append order if present
        if (!orders.isEmpty()) {
            queryString.append(" order by ");
            queryString.append(QueryResolver.toOrderClause(TEMP_TABLE, orders));
        }

        // end script
        queryString.append(" ) final;");

        // create query and pagination
        Query query = createNativeQuery(departmentId, startDate, endDate, confirmedStatus, returnedStatus, confirmedStatusWithoutReturned, queryString.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            OrderSumModel emptyModel = new OrderSumModel();
            emptyModel.setOrders(0L);
            emptyModel.setConfirmedOrders(0L);
            emptyModel.setReturnedOrders(0L);
            emptyModel.setSuccessOrders(0L);
            emptyModel.setRevenue(BigDecimal.ZERO);
            emptyModel.setTrueRevenue(BigDecimal.ZERO);
            return emptyModel;
        }

        Object[] row = results.get(0);
        OrderSumModel sumModel = new OrderSumModel();
        sumModel.setOrders(row[0] != null ? ((Number) row[0]).longValue() : 0L);
        sumModel.setConfirmedOrders(row[1] != null ? ((Number) row[1]).longValue() : 0L);
        sumModel.setReturnedOrders(row[2] != null ? ((Number) row[2]).longValue() : 0L);
        sumModel.setSuccessOrders(row[3] != null ? ((Number) row[3]).longValue() : 0L);
        sumModel.setRevenue(row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO);
        sumModel.setTrueRevenue(row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO);

        return sumModel;
    }

    public List<OrderQueryByDateModel> reportOrderByDateOfProduct(String departmentId, String productId,
                                                               LocalDate startDate, LocalDate endDate,
                                                               ConfirmOrderWhen confirmOrderWhen, ReturnOrderWhen returnOrderWhen) {
        List<String> confirmedStatus = confirmOrderWhen.getOrderStatus();
        List<String> returnedStatus = returnOrderWhen.getOrderStatus();
        List<String> confirmedStatusWithoutReturned = confirmedStatus.stream().filter(s -> !returnedStatus.contains(s)).toList();

        String queryString = """
                select product_id,
                       order_date,
                       SUM(s.total)                                                           AS orders,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.total ELSE 0 END)   AS confirmed_orders,
                       SUM(CASE WHEN s.status in :returned_status THEN s.total ELSE 0 END)    AS returned_orders,
                       SUM(CASE WHEN s.status in :success_status THEN s.total ELSE 0 END)     AS success_orders,
                       SUM(CASE WHEN s.status in :confirmed_status THEN s.revenue ELSE 0 END) AS revenue,
                       SUM(CASE
                               WHEN s.status in :confirmed_status_without_returned
                                   THEN s.revenue
                               ELSE 0 END)                                                    AS true_revenue
                from (select p.product_id                        as product_id,
                             TO_CHAR(o.created_at, 'DD/MM/YYYY') AS order_date,
                             o.status,
                             COUNT(1)                            AS total,
                             SUM(oi.total_price)                 AS revenue
                      from product_manager.products p
                               left join product_manager.pos pos on p.pos_id = pos.id
                               left join product_manager.order_item oi on oi.product_id = p.product_id
                               left join product_manager.orders o on oi.order_id = o.order_id
                      where pos.department_id = :department_id
                        and pos.status in ('ACTIVE', 'PROCESSING')
                        and p.product_id = :product_id
                        and o.created_at >= :start_date
                        and o.created_at < :end_date
                      group by p.product_id, TO_CHAR(o.created_at, 'DD/MM/YYYY'), o.status) s
                group by s.product_id, s.order_date
                order by s.order_date;
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
