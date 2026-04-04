package com.aktor.core.model;

import com.aktor.core.SearchCriteria;

import java.util.Objects;
import java.util.function.Function;

abstract class SqlDialectAbstract
implements SqlDialect
{
    protected final String quoteField(final String fieldName)
    {
        return quoteStart() + fieldName + quoteEnd();
    }

    protected final String toConflictAssignments(
        final String keyFieldName,
        final String[] fieldNames,
        final Function<String, String> valueExpression
    )
    {
        Objects.requireNonNull(fieldNames);
        Objects.requireNonNull(valueExpression);

        final StringBuilder output = new StringBuilder();
        for (final String fieldName : fieldNames)
        {
            if (fieldName == null || fieldName.isBlank() || fieldName.equalsIgnoreCase(keyFieldName))
            {
                continue;
            }
            if (!output.isEmpty())
            {
                output.append(", ");
            }
            output.append(valueExpression.apply(fieldName));
        }
        return output.toString();
    }

    @Override
    public String paginationClause(final SearchCriteria searchCriteria, final String orderByClause)
    {
        Objects.requireNonNull(searchCriteria);
        final long offset = Math.max(
            0L,
            ((long) searchCriteria.currentPage() - 1L) * (long) searchCriteria.pageSize()
        );
        return "LIMIT "
            + searchCriteria.pageSize()
            + " OFFSET "
            + offset;
    }
}
