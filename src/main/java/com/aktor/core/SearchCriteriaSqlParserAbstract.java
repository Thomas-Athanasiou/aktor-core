package com.aktor.core;

import com.aktor.core.model.FieldResolver;
import com.aktor.core.util.CsvValuesUtil;
import com.aktor.core.value.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class SearchCriteriaSqlParserAbstract
implements Converter<SearchCriteria, String>
{
    protected final String tableName;
    protected final String start;
    protected final String end;
    protected final FieldResolver fieldResolver;

    protected SearchCriteriaSqlParserAbstract(
        final String tableName,
        final String start,
        final String end,
        final FieldResolver fieldResolver
    )
    {
        super();
        this.tableName = Objects.requireNonNull(tableName);
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);
        this.fieldResolver = Objects.requireNonNull(fieldResolver);
    }

    protected static String joinParts(final Iterable<String> parts)
    {
        final StringBuilder builder = new StringBuilder();
        for (final String part : parts)
        {
            if (part != null && !part.isBlank())
            {
                if (builder.length() > 0)
                {
                    builder.append(' ');
                }
                builder.append(part);
            }
        }
        return builder.toString();
    }

    protected final String selectSql(
        final String projection,
        final SearchCriteria searchCriteria,
        final String... trailingParts
    )
    {
        final List<String> parts = new ArrayList<>(List.of("SELECT", projection, "FROM", tableReference(), groupsToSql(searchCriteria.filterGroups())));
        Collections.addAll(parts, trailingParts);
        return joinParts(parts);
    }

    protected final String tableReference()
    {
        return start + tableName + end;
    }

    protected final String groupsToSql(final FilterGroup[] groups)
    {
        if (groups.length < 1)
        {
            return "";
        }
        final StringBuilder builder = new StringBuilder("WHERE (");
        for (int index = 0; index < groups.length; index++)
        {
            if (index > 0)
            {
                builder.append(" AND ");
            }
            builder.append(groupToSql(groups[index]));
        }
        builder.append(')');
        return builder.toString();
    }

    private String groupToSql(final FilterGroup filterGroup)
    {
        final Filter[] filters = filterGroup.filters();
        final StringBuilder builder = new StringBuilder("(");
        for (int index = 0; index < filters.length; index++)
        {
            if (index > 0)
            {
                builder.append(" OR ");
            }
            builder.append(filterToSql(filters[index]));
        }
        builder.append(')');
        return builder.toString();
    }

    private String filterToSql(final Filter filter)
    {
        final String field = start + fieldResolver.resolve(filter.field()) + end;

        return switch (filter.conditionType())
        {
            case NOT_EQUALS -> field + " != ?";
            case GREATER_THAN -> field + " > ?";
            case FROM -> field + " >= ?";
            case GREATER_THAN_OR_EQUALS, MORE_OR_EQUALS -> field + " >= ?";
            case LESS_THAN -> field + " < ?";
            case TO -> field + " <= ?";
            case LESS_THAN_OR_EQUALS -> field + " <= ?";
            case LIKE -> field + " LIKE ?";
            case NOT_LIKE -> field + " NOT LIKE ?";
            case IN -> inClause(field, filter.value(), false);
            case NOT_IN -> inClause(field, filter.value(), true);
            case IS_NULL -> field + " IS NULL";
            case IS_NOT_NULL -> field + " IS NOT NULL";
            default -> field + " = ?";
        };
    }

    private static String inClause(final String field, final String commaSeparated, final boolean negative)
    {
        final String[] parts = CsvValuesUtil.split(commaSeparated);
        return parts.length == 0
            ? (negative ? "1 = 1" : "1 = 0")
            : field + (negative ? " NOT IN " : " IN ") + questionMarks(parts);
    }

    private static String questionMarks(final String[] parts)
    {
        final StringBuilder builder = new StringBuilder("(");
        for (int index = 0; index < parts.length; index++)
        {
            if (index > 0)
            {
                builder.append(',');
            }
            builder.append('?');
        }
        builder.append(')');
        return builder.toString();
    }

    protected final String convertSql(final SqlSupplier sqlSupplier) throws com.aktor.core.exception.ConversionException
    {
        try
        {
            return sqlSupplier.get();
        }
        catch (final IllegalArgumentException exception)
        {
            throw new com.aktor.core.exception.ConversionException(exception);
        }
    }

    @FunctionalInterface
    protected interface SqlSupplier
    {
        String get();
    }
}
