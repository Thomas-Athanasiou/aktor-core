package com.aktor.core;

import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.FieldResolver;

import java.util.Objects;

final class SqlParserUtil
{
    private SqlParserUtil()
    {
        super();
    }

    static String keyPredicate(final String[] keyFieldNames, final String start, final String end)
    {
        final String[] fieldNames = requireKeyFieldNames(keyFieldNames);
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < fieldNames.length; index++)
        {
            if (index > 0)
            {
                builder.append(" AND ");
            }
            builder.append(start).append(fieldNames[index]).append(end).append(" = ?");
        }
        return builder.toString();
    }

    static String[] requireKeyFieldNames(final String[] keyFieldNames)
    {
        final String[] safeKeyFieldNames = Objects.requireNonNull(keyFieldNames);
        if (safeKeyFieldNames.length < 1)
        {
            throw new IllegalArgumentException("keyFieldNames cannot be empty");
        }
        return safeKeyFieldNames;
    }

    static String selectByKeySql(final String tableName, final String[] keyFieldNames, final String start, final String end)
    {
        return "SELECT * FROM " + start + tableName + end + " WHERE " + keyPredicate(keyFieldNames, start, end);
    }

    static String deleteByKeySql(final String tableName, final String[] keyFieldNames, final String start, final String end)
    {
        return "DELETE FROM " + start + tableName + end + " WHERE " + keyPredicate(keyFieldNames, start, end);
    }

    static String sortOrdersToSql(
        final SortOrder[] orders,
        final String start,
        final String end,
        final FieldNormalizer fieldResolver
    )
    {
        if (orders.length < 1)
        {
            return "";
        }
        final StringBuilder builder = new StringBuilder("ORDER BY ");
        for (int index = 0; index < orders.length; index++)
        {
            if (index > 0)
            {
                builder.append(", ");
            }
            final SortOrder order = orders[index];
            builder.append(start)
                .append(fieldResolver.resolve(order.field()))
                .append(end)
                .append(' ')
                .append(order.direction() ? "ASC" : "DESC");
        }
        return builder.toString();
    }
}
