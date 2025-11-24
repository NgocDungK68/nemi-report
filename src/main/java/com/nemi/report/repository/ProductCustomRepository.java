package com.nemi.report.repository;

import com.nemi.report.constant.ColumnDataType;
import com.nemi.report.constant.ProductSource;
import com.nemi.report.model.OrderParameter;
import com.nemi.report.model.QueryParameter;
import com.nemi.report.model.config.ColumnConfig;
import com.nemi.report.model.response.PageCountData;
import com.nemi.report.util.QueryResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.NativeQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j

public class ProductCustomRepository {
    private final EntityManager em;

    private static final String BASE_WHERE_CLAUSE = "where o.department_id = :department and oi.created_at <= :endDate and oi.created_at >= :startDate";

    private static final String PRODUCT_WHERE_CLAUSE = "where o.department_id = :department and oi.created_at <= :endDate and oi.created_at >= :startDate and p.product_id = :productId";

    private String buildFromClause(List<ProductSource> joinSources) {
        StringBuilder sql = new StringBuilder(" FROM product_manager.products p ");

        for (ProductSource source : joinSources) {
            if (source.equals(ProductSource.ORDER_ITEM)) {
                sql.append("""
                        LEFT JOIN product_manager.order_item oi 
                            ON oi.product_name = p.name
                                AND oi.pos_id = p.pos_id
                                AND oi.product_id = p.product_id
                        """);
            } else if (source.equals(ProductSource.ORDER)) {
                sql.append("""
                        LEFT JOIN product_manager.orders o 
                            ON o.order_id
                                    = oi.order_id
                        """);
            }
            // sau này có thể thêm bảng khác mà không đụng code gốc
            // else if (source.equals(ProductSource.WAREHOUSE)) { ... }
        }

        return sql.toString();
    }


    @SuppressWarnings({"unchecked"})
    public List<Map<String, Object>> search(Set<ColumnConfig> columns, Set<QueryParameter> queryParameters, Set<OrderParameter> orderParameters, LocalDate startDate, LocalDate endDate, PageRequest pageRequest, String departmentId, String productId) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select * from (select p.product_id, p.name, p.status ");

            if (ObjectUtils.isNotEmpty(productId)) {
                sql.append(", TO_CHAR(oi.created_at, 'YYYY-MM-DD') as created_at ");
            }

//            if (adsTab.equals(AdsTab.AD_ACCOUNT)) {
//                sql.append(", a.account_status as status_account, a.currency as currency_default ");
//            } else if (adsTab.equals(AdsTab.CAMPAIGN)) {
//                sql.append(", a.status, MAX(ac.account_status) as status_account, MAX(ac.currency) as currency_default, a.objective as objective_default ");
//            } else {
//                sql.append(", p.status, MAX(ac.account_status) as status_account, MAX(ac.currency) as currency_default, a.campaign ->> 'objective' as objective_default ");
//            }

            buildSelectAndFromAndWhereClause(sql, columns, queryParameters, productId);

            sql.append(" order by ");

            // append order by clause
            ColumnConfig firstColumn = columns.stream().findFirst().orElse(null);
            if (ObjectUtils.isEmpty(pageRequest) && ObjectUtils.isNotEmpty(firstColumn)) {
                sql.append(String.format("s.%s desc NULLS LAST", firstColumn.getCode()));
            } else if (ObjectUtils.isEmpty(orderParameters) || !StringUtils.isEmpty(productId)) {
                sql.append("s.created_at desc");
            } else {
                String orderByClause = orderParameters.stream()
                        .map(order -> String.format("s.%s %s NULLS LAST", order.getColumn().getMapping(), order.getDirection()))
                        .collect(Collectors.joining(", "));
                sql.append(orderByClause);
            }

            // build query
            Query query = buildQuery(sql, departmentId, startDate, endDate, productId);

            if (ObjectUtils.isNotEmpty(pageRequest)) {
                query.setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize());
                query.setMaxResults(pageRequest.getPageSize());
            } else {
                query.setMaxResults(10);   // LIMIT 10
            }

            setResultMapping(query);

            log.debug("SQL search: {}", sql);

            return (List<Map<String, Object>>) query.getResultList();
        } catch (Exception e) {
            log.error("Error in search", e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setResultMapping(Query query) {
        query.unwrap(NativeQuery.class)
                .setTupleTransformer((tuple, aliases) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < aliases.length; i++) {
                        row.put(aliases[i], tuple[i]);
                    }
                    return row;
                });
    }

    public PageCountData count(Set<QueryParameter> queryParameters, LocalDate startDate, LocalDate endDate, PageRequest pageRequest, String departmentId, String productId) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(1) from (select * from (select p.product_id ");

            // append from and where clause
            LinkedHashSet<ColumnConfig> selectColumns = new LinkedHashSet<>();
            buildSelectAndFromAndWhereClause(sql, selectColumns, queryParameters, productId);

            sql.append(" ) as count");

            Query query = buildQuery(sql, departmentId, startDate, endDate, productId);

            log.debug("SQL count: {}", sql);

            long count = ((Number) query.getSingleResult()).longValue();
            int totalPages = 1;
            if (ObjectUtils.isNotEmpty(pageRequest)) {
                totalPages = (int) Math.ceil(count / (double) pageRequest.getPageSize());
            }
            return new PageCountData(count, totalPages);
        } catch (Exception e) {
            log.error("Error in count", e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Object> summary(Set<ColumnConfig> columns, Set<QueryParameter> queryParameters, LocalDate startDate, LocalDate endDate, String departmentId, String productId) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("select p.product_id ");

            // build select SUM or AVG
            StringBuilder summarySql = new StringBuilder();
            summarySql.append("select ");

            if (ObjectUtils.isNotEmpty(productId)) {
                sql.append(", TO_CHAR(oi.created_at, 'YYYY-MM-DD') as created_at ");
                summarySql.append("s.created_at, ");
            }

            // TODO: fix summary code
//            if (ObjectUtils.isNotEmpty(columns)) {
//                summarySql.append(", ");
//            }

            // append from and where clause
            buildSelectAndFromAndWhereClause(sql, columns, queryParameters, productId);

            String sumCols = columns.stream()
                    .map(col -> {
                        if (col.getRequiredForAvg() != null) {
                            return col.getAvgFormula() + String.format(" as \"%s\" ", col.getCode());
                        }
                        return String.format(" %s(s.%s) as \"%s\" ", col.getSummaryType().name(), col.getCode(), col.getCode());
                    })
                    .collect(Collectors.joining(","));
            summarySql.append(sumCols);
            summarySql.append(String.format(" from (%s ", sql));
//            summarySql.append("group by s.product_id");
            if (ObjectUtils.isNotEmpty(productId)) {
                summarySql.append(" group by s.created_at"); // bổ sung GROUP BY
            }

            Query query = buildQuery(summarySql, departmentId, startDate, endDate, productId);
            setResultMapping(query);

            log.debug("SQL summary: {}", summarySql);
            List<Map<String, Object>> summary = query.getResultList();
            if (summary.isEmpty()) {
                return Collections.emptyMap();  // hoặc null tùy bạn
            }
            return summary.get(0);
        } catch (Exception e) {
            log.error("Error in summary", e);
            throw new RuntimeException(e);
        }
    }

    private Query buildQuery(StringBuilder sqlBuilder, String departmentId, LocalDate startDate, LocalDate endDate, String productId) {
        Query query = em.createNativeQuery(sqlBuilder.toString());
        query.setParameter("department", departmentId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        if (StringUtils.isNotEmpty(productId)) {
            query.setParameter("productId", productId);
        }
        return query;
    }

    private void buildSelectAndFromAndWhereClause(StringBuilder sql, Set<ColumnConfig> columns, Set<QueryParameter> queryParameters, String productId) {
        Set<ColumnConfig> selectColumns = new LinkedHashSet<>(columns);
        List<QueryParameter> queryInMains = new ArrayList<>();
        List<QueryParameter> queryInInsights = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(queryParameters)) {
            queryParameters.forEach(qp -> {
                ColumnConfig col = qp.getColumn();
                if (col.getSource().equals(ProductSource.MAIN)) {
                    queryInMains.add(qp);
                } else {
                    queryInInsights.add(qp);
                    selectColumns.add(col);
                }
            });
        }

        // For select column here
        buildSelectAndFromClause(sql, selectColumns, productId);

        // append main table where clause
        queryInMains.forEach(qM -> {
            sql.append("\n and ");
            sql.append(QueryResolver.buildSqlCondition(qM));
        });

        // GROUP BY
        if (StringUtils.isEmpty(productId)) {
            sql.append(" GROUP BY p.product_id, p.pos_id) s ");
        } else {
            sql.append(" GROUP BY p.product_id, p.pos_id, TO_CHAR(oi.created_at, 'YYYY-MM-DD')) s ");
        }

        // append where clause in insight table
        if (!queryInInsights.isEmpty())
            buildWhereClause(sql, queryInInsights);
    }

    private void buildSelectAndFromClause(StringBuilder sql, Set<ColumnConfig> summaryColumns, String productId) {
        summaryColumns.forEach(column -> {
            if (StringUtils.isNoneEmpty(column.getFormula())) {
                sql.append(String.format(", %s as \"%s\"", column.getFormula(), column.getCode()));
            } else {
                appendSelectColumn(sql, column);
            }
            sql.append("\n");
        });

        sql.append(buildFromClause(List.of(ProductSource.ORDER_ITEM, ProductSource.ORDER)));
        sql.append("\n");
        // For other tabs, use the base where clause
        if (StringUtils.isEmpty(productId)) {
            sql.append(BASE_WHERE_CLAUSE);
        } else {
            sql.append(PRODUCT_WHERE_CLAUSE);
        }

    }

    private void appendSelectColumn(StringBuilder sql, ColumnConfig column) {
        boolean isInMainTable = column.getSource().equals(ProductSource.MAIN);
        String source = resolveSourceAlias(column.getSource());
        String mapping = QueryResolver.getColumnMapping(column);
        String selectField = QueryResolver.convertToJsonbAccess(source, mapping, column.getType());
        if (isInMainTable) {
            sql.append(String.format(", %s as \"%s\"", selectField, column.getCode()));
        } else {
            if (column.getType().equals(ColumnDataType.TEXT) || column.getType().equals(ColumnDataType.ENUM)) {
                sql.append(String.format(", MAX(%s) as \"%s\"", selectField, column.getCode()));
            } else {
                sql.append(String.format(", SUM(%s) as \"%s\"", selectField, column.getCode()));
            }
        }
    }

    private String resolveSourceAlias(ProductSource source) {
        return switch (source) {
            case MAIN -> "p";
            case ORDER -> "o";
            case VIEW_ONLY -> "i";
            case ORDER_ITEM -> "oi";
            case INSIGHT -> "i";
            case ADS -> "ad";
        };
    }


    private void buildWhereClause(StringBuilder sql, List<QueryParameter> queryParameters) {
        sql.append("where ");
        sql.append(queryParameters.stream()
                .map(QueryResolver::buildSqlCondition)
                .collect(Collectors.joining(" \nand ")));
    }
}
