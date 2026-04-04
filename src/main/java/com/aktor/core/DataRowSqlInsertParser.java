package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.util.DataRowSqlStatementUtil;

public class DataRowSqlInsertParser<Item>
extends DataRowSqlParserBase<Item>
{
    public DataRowSqlInsertParser(
        final String table,
        final String start,
        final String end,
        final Converter<Item, DataRow> converter
    )
    {
        super(table, start, end, converter);
    }

    @Override
    public String convert(final Item input) throws ConversionException
    {
        final Value[] values = requireValues(input);

        return  "INSERT INTO " + quotedTable()
            + "(" + DataRowSqlStatementUtil.columns(values, start, end) + ")"
            + "VALUES (" + DataRowSqlStatementUtil.placeholders(values.length) + ")";
    }
}
