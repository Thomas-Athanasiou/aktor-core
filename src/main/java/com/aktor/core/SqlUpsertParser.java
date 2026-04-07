package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;
import com.aktor.core.util.SqlStatementUtil;

import java.util.Objects;

public final class SqlUpsertParser<Item>
extends SqlParserBase<Item>
{
    private final String keyField;

    private final SqlDialect dialect;

    private SqlUpsertParser(
        final String tableName,
        final String keyField,
        final SqlDialect dialect,
        final Converter<Item, Row> converter
    )
    {
        super(tableName, Objects.requireNonNull(dialect).quoteStart(), dialect.quoteEnd(), converter);
        this.keyField = Objects.requireNonNull(keyField);
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
            + SqlStatementUtil.columns(values, start, end)
            + ")"
            + " VALUES ("
            + SqlStatementUtil.placeholders(values.length)
            + ") "
            + dialect.upsertClause(keyField, fields(values));
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

    public static <Item> Converter<Item, String> of(
        final String table,
        final String driver,
        final Converter<Item, Row> converter,
        final FieldNormalizer fieldNormalizer
        )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        if (!dialect.supportsUpsert())
        {
            return null;
        }
        return new SqlUpsertParser<>(table, fieldNormalizer.resolve(LOGICAL_KEY_FIELD_NAME), dialect, converter);
    }
}
