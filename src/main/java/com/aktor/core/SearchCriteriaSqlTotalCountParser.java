package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;

public final class SearchCriteriaSqlTotalCountParser
extends SearchCriteriaSqlParser
{
    private SearchCriteriaSqlTotalCountParser(
        final String tableName,
        final String start,
        final String end,
        final FieldNormalizer fieldResolver
    )
    {
        super(tableName, start, end, fieldResolver);
    }

    @Override
    public String convert(final SearchCriteria searchCriteria) throws ConversionException
    {
        return selectSql("COUNT(*)", searchCriteria);
    }

    public static Converter<SearchCriteria, String> of(
        final String table,
        final String driver,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        return new SearchCriteriaSqlTotalCountParser(
            table,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            fieldResolver
        );
    }
}
