package com.aktor.core.model;

import com.aktor.core.SearchCriteria;

public final class SqlDialectSqlServer
extends SqlDialectAbstract
{
    @Override
    public String name()
    {
        return "sqlserver";
    }

    @Override
    public String quoteStart()
    {
        return "[";
    }

    @Override
    public String quoteEnd()
    {
        return "]";
    }

    @Override
    public boolean supportsUpsert()
    {
        return false;
    }

    @Override
    public String upsertClause(final String keyFieldName, final String[] fieldNames)
    {
        throw new UnsupportedOperationException("SQL Server dialect upsert is not supported");
    }

    @Override
    public String paginationClause(final SearchCriteria searchCriteria, final String orderByClause)
    {
        final String orderBy = orderByClause == null || orderByClause.isBlank() ? "ORDER BY (SELECT 1)" : orderByClause;
        final long offset = Math.max(
            0L,
            ((long) searchCriteria.currentPage() - 1L) * (long) searchCriteria.pageSize()
        );
        return orderBy
            + (orderBy.isBlank() ? "" : " ")
            + "OFFSET "
            + offset
            + " ROWS FETCH NEXT "
            + searchCriteria.pageSize()
            + " ROWS ONLY";
    }

    @Override
    public boolean paginationClauseIncludesOrderBy()
    {
        return true;
    }
}
