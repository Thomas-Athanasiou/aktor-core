package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldResolver;

public class SearchCriteriaSqlTotalCountParser
extends SearchCriteriaSqlParserAbstract
{
    public SearchCriteriaSqlTotalCountParser(
        final String tableName,
        final String start,
        final String end,
        final FieldResolver fieldResolver
    )
    {
        super(tableName, start, end, fieldResolver);
    }

    @Override
    public String convert(final SearchCriteria searchCriteria) throws ConversionException
    {
        return convertSql(() -> selectSql("COUNT(*)", searchCriteria));
    }
}
