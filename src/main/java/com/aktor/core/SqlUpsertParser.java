package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;
import com.aktor.core.util.DataRowSqlStatementUtil;

import java.util.Objects;

public final class DataRowSqlUpsertParser<Item>
extends DataRowSqlParserBase<Item>
{
    private final String keyFieldName;

    private final SqlDialect dialect;

    private DataRowSqlUpsertParser(
        final String tableName,
        final String keyField,
        final SqlDialect dialect,
        final Converter<Item, DataRow> converter
    )
    {
        super(tableName, Objects.requireNonNull(dialect).quoteStart(), dialect.quoteEnd(), converter);
        this.keyFieldName = Objects.requireNonNull(keyField);
        this.dialect = dialect;
    }

    @Override
    public String convert(final Item input) throws ConversionException
    {
        if (!dialect.supportsUpsert())
        {
            throw new ConversionException("SQL dialect '" + dialect.name() + "' does not support native upsert");
        }

        final Value[] values = requireValues(input);

        return "INSERT INTO "
            + quotedTable()
            + "("
            + DataRowSqlStatementUtil.columns(values, start, end)
            + ")"
            + " VALUES ("
            + DataRowSqlStatementUtil.placeholders(values.length)
            + ") "
            + dialect.upsertClause(keyFieldName, fields(values));
    }

    private static String[] fields(final Value[] values)
    {
        final String[] fields = new String[values.length];
        for (int index = 0; index < values.length; index++)
        {
            fields[index] = values[index].field();
        }
        return fields;
    }

    private static <Item> Converter<Item, String> of(
        final String table,
        final String keyField,
        final Converter<Item, DataRow> converter,
        final String driver
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        if (!dialect.supportsUpsert())
        {
            return null;
        }
        return new DataRowSqlUpsertParser<>(table, keyField, dialect, converter);
    }
}
