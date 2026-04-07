package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;

import java.util.Objects;

public final class SearchCriteriaSqlSearchParser
extends SearchCriteriaSqlParser
{
    private final SqlDialect dialect;

    private SearchCriteriaSqlSearchParser(
        final String tableName,
        final SqlDialect dialect,
        final FieldNormalizer fieldResolver
    )
    {
        super(tableName, dialect.quoteStart(), dialect.quoteEnd(), fieldResolver);
        this.dialect = Objects.requireNonNull(dialect);
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
                    fieldNormalizer
                );
                final String paginationSql = dialect.paginationClause(searchCriteria, sortOrdersSql);
                return dialect.paginationClauseIncludesOrderBy()
                    ? selectSql("*", searchCriteria, paginationSql)
                    : selectSql("*", searchCriteria, sortOrdersSql, paginationSql);
            }
        );
    }

    public static Converter<SearchCriteria, String> of(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        return new SearchCriteriaSqlSearchParser(table, SqlDialectResolver.of(driver), fieldResolver);
    }
}
