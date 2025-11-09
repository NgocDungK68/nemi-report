package com.nemi.report.util;


import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.report.constant.ColumnDataType;
import com.nemi.report.exception.ValidationAlertCode;
import com.nemi.report.model.QueryParameter;
import com.nemi.report.model.config.ColumnConfig;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@UtilityClass
public class QueryResolver {

    public static String convertToJsonbAccess(String source, String input, ColumnDataType type) {
        String[] parts = input.split(">>>");
        if (parts.length == 1) {
            if (StringUtils.isNotEmpty(source)) {
                return source + "." + input;
            } else {
                return input;
            }
        } else {
            return String.format("CAST(jsonb_array_find_by_action(%s.%s, '%s') ->> 'value' as %s)", source, parts[0], parts[1], type.toString().toLowerCase());
        }
    }

    public static String buildSqlCondition(QueryParameter queryParameter) {
        ColumnConfig column = queryParameter.getColumn();

        String field;

        switch (column.getSource()) {
            case MAIN -> {
                if (Objects.nonNull(column.getCustomFilter())) {
                    field = "p." + column.getCustomFilter();
                } else if (Objects.nonNull(column.getFormula())) {
                    field = column.getFormula();
                } else {
                    field = "p." + QueryResolver.getColumnMapping(column);
                }
            }
            case ORDER -> {
                field = "o." + QueryResolver.getColumnMapping(column);
            }
            case ORDER_ITEM -> {
                field = "oi." + QueryResolver.getColumnMapping(column);
            }
            default -> {
                throw new IllegalArgumentException("Unknown product source: " + column.getSource());
            }
        }


        switch (queryParameter.getFilterType()) {
            case EQUAL -> {
                return field + " = " + String.format("'%s'", queryParameter.getValue().get(0));
            }
            case FROM -> {
                return field + " >= " + String.format("'%s'", queryParameter.getValue().get(0));
            }
            case WITHIN -> {
                return field + " <= " + String.format("'%s'", queryParameter.getValue().get(0));
            }
            case BETWEEN -> {
                if (queryParameter.getValue().size() < 2) {
                    throw new ValidationException(AlertMessages.alert(ValidationAlertCode.DATA_INVALID));
                }
                return field + " BETWEEN " + String.format("'%s'", queryParameter.getValue().get(0)) + " AND " + String.format("'%s'", queryParameter.getValue().get(1));
            }
            case OUTSIDE -> {
                if (queryParameter.getValue().size() < 2) {
                    throw new ValidationException(AlertMessages.alert(ValidationAlertCode.DATA_INVALID));
                }
                return field + " NOT BETWEEN " + String.format("'%s'", queryParameter.getValue().get(0)) + " AND " + String.format("'%s'", queryParameter.getValue().get(1));
            }
            case IN -> {
                String values = String.join("', '", queryParameter.getValue());
                return field + " IN ('" + values + "')";
            }
            case NOT_IN -> {
                String values = String.join("', '", queryParameter.getValue());
                return field + " NOT IN ('" + values + "')";
            }
            case LIKE -> {
                return field + " ILIKE " + "'%" + queryParameter.getValue().get(0) + "%'";
            }
            case NOT_LIKE -> {
                return field + " NOT ILIKE " + "'%" + queryParameter.getValue().get(0) + "%'";
            }
            default -> {
                return "";
            }
        }
    }

    public static String getColumnMapping(ColumnConfig column) {
        String mapping = column.getMapping();
        return column.getMapping();
    }
}
