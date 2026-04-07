package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;
import com.aktor.core.util.SqlStatementUtil;

import java.util.Arrays;

public final class SqlUpdateParser<Item>
extends SqlParserBase<Item>
{
    private final String[] keyFieldNames;

    private SqlUpdateParser(
        final String tableName,
        final String[] keyFields,
        final String start,
        final String end,
        final Converter<Item, Row> converter
    )
    {
        super(tableName, start, end, converter);
        this.keyFieldNames = Arrays.copyOf(SqlParserUtil.requireKeyFieldNames(keyFields), keyFields.length);
    }

    @Override
    public String convert(final Item input) throws ConversionException
    {
        final Value[] values = requireValues(input);
        return "UPDATE "
            + quotedTable()
            + " SET "
            + SqlStatementUtil.assignments(values, start, end)
            + " WHERE "
            + SqlParserUtil.keyPredicate(keyFieldNames, start, end);
    }

    public static <Item> SqlUpdateParser<Item> of(
        final String table,
        final String driver,
        final Converter<Item, Row> converter,
        final FieldNormalizer fieldResolver
    )
    {
        return of(
            table,
            driver,
            new String[] {LOGICAL_KEY_FIELD_NAME},
            converter,
            fieldResolver
        );
    }

    public static <Item> SqlUpdateParser<Item> of(
        final String table,
        final String driver,
        final String[] logicalKeys,
        final Converter<Item, Row> converter,
        final FieldNormalizer fieldResolver
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        return new SqlUpdateParser<>(
            table,
            Arrays.stream(logicalKeys).map(fieldResolver::resolve).toArray(String[]::new),
            dialect.quoteStart(),
            dialect.quoteEnd(),
            converter
        );
    }
}
