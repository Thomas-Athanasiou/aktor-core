package com.aktor.core.model;

import com.aktor.core.SearchCriteria;

public interface SqlDialect
{
    String name();

    String quoteStart();

    String quoteEnd();

    boolean supportsUpsert();

    String upsertClause(String keyFieldName, String[] fieldNames);

    String paginationClause(SearchCriteria searchCriteria, String orderByClause);

    default boolean paginationClauseIncludesOrderBy()
    {
        return false;
    }
}
