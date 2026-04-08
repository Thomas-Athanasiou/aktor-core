package com.aktor.core.model;

import com.aktor.core.Model;
import com.aktor.core.SearchCriteria;

public interface SqlDialect
extends Model
{
    String name();

    String quoteStart();

    String quoteEnd();

    boolean supportsUpsert();

    String upsertClause(String keyField, String[] fieldNames);

    String paginationClause(SearchCriteria searchCriteria, String orderByClause);

    default boolean paginationClauseIncludesOrderBy()
    {
        return false;
    }
}
