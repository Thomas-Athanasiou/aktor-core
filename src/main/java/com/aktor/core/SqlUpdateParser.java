package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.util.DataRowSqlStatementUtil;

import java.util.Arrays;

public class DataRowSqlUpdateParser<Item>
extends DataRowSqlParserBase<Item>
{
    private final String[] keyFieldNames;

    public DataRowSqlUpdateParser(
        final String tableName,
        final String keyField,
        final String start,
        final String end,
        final Converter<Item, DataRow> converter
    )
    {
        this(tableName, new String[] {keyField}, start, end, converter);
    }

    DataRowSqlUpdateParser(
        final String tableName,
        final String[] keyFieldNames,
        final String start,
        final String end,
        final Converter<Item, DataRow> converter
    )
    {
        super(tableName, start, end, converter);
        this.keyFieldNames = Arrays.copyOf(SqlParserUtil.requireKeyFieldNames(keyFieldNames), keyFieldNames.length);
    }

    @Override
    public String convert(final Item input) throws ConversionException
    {
        final Value[] values = requireValues(input);
        return "UPDATE "
            + quotedTable()
            + " SET "
            + DataRowSqlStatementUtil.assignments(values, start, end)
            + " WHERE "
            + SqlParserUtil.keyPredicate(keyFieldNames, start, end);
    }



    public static <Item> DataRowSqlUpdateParser<Item> ofDataRowUpdateParser(
        final String table,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        return ofDataRowUpdateParser(table, LOGICAL_KEY_FIELD_NAME, converter, driver);
    }

    private static <Item> DataRowSqlUpdateParser<Item> ofDataRowUpdateParser(
        final String table,
        final String keyField,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        final SqlDialect dialect = ofDialect(driver);
        return new DataRowSqlUpdateParser<>(
            table,
            keyField,
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }
}
