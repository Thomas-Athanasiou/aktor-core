package com.aktor.core.model;

import com.aktor.core.Model;
import com.aktor.core.SearchCriteria;

// TODO MAKE DATA?
public interface SqlDialect
extends Model
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
