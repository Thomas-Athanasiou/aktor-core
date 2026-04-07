package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;
import com.aktor.core.util.SqlStatementUtil;

public class SqlInsertParser<Item>
extends SqlParserBase<Item>
{
    public SqlInsertParser(
        final String table,
        final String start,
        final String end,
        final Converter<Item, Row> converter
    )
    {
        super(table, start, end, converter);
    }

    @Override
    public String convert(final Item input) throws ConversionException
    {
        final Value[] values = requireValues(input);

        return  "INSERT INTO " + quotedTable()
            + "(" + SqlStatementUtil.columns(values, start, end) + ")"
            + "VALUES (" + SqlStatementUtil.placeholders(values.length) + ")";
    }

    public static <Item> SqlInsertParser<Item> of(
        final String table,
        final String driver,
        final Converter<Item, Row> converter
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        return new SqlInsertParser<>(
            table,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }
}
