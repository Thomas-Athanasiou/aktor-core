package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldResolver;
import com.aktor.core.model.SqlDialect;

import java.util.Objects;

public class SearchCriteriaSqlSearchParser
extends SearchCriteriaSqlParserAbstract
{
    private final SqlDialect sqlDialect;

    public SearchCriteriaSqlSearchParser(
        final String tableName,
        final String start,
        final String end,
        final SqlDialect sqlDialect,
        final FieldResolver fieldResolver
    )
    {
        super(tableName, start, end, fieldResolver);
        this.sqlDialect = Objects.requireNonNull(sqlDialect);
    }

    @Override
    public String convert(final SearchCriteria searchCriteria) throws ConversionException
    {
        return convertSql(
            () -> {
            final String sortOrdersSql = SqlParserUtil.sortOrdersToSql(
                searchCriteria.sortOrders(),
                start,
                end,
                fieldResolver
            );
            final String paginationSql = sqlDialect.paginationClause(searchCriteria, sortOrdersSql);
            return sqlDialect.paginationClauseIncludesOrderBy()
                ? selectSql("*", searchCriteria, paginationSql)
                : selectSql("*", searchCriteria, sortOrdersSql, paginationSql);
            }
        );
    }
}
