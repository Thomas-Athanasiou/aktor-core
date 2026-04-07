package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;

public class KeySqlDeleteParser<Key>
extends KeySqlParserBase<Key>
{
    public KeySqlDeleteParser(final String tableName, final String keyFieldName, final String start, final String end)
    {
        this(tableName, new String[] {keyFieldName}, start, end);
    }

    KeySqlDeleteParser(final String tableName, final String[] keyFieldNames, final String start, final String end)
    {
        super(tableName, keyFieldNames, start, end);
    }

    @Override
    public String convert(final Key input) throws ConversionException
    {
        requireKey(input);
        return SqlParserUtil.deleteByKeySql(table, keyFields, start, end);
    }

    public static <Key> KeySqlDeleteParser<Key> of(
        final String table,
        final String driver,
        final FieldNormalizer fieldNormalizer
    )
    {
        return of(table, driver, fieldNormalizer.resolve(LOGICAL_KEY_FIELD_NAME));
    }

    private static <Key> KeySqlDeleteParser<Key> of(
        final String table,
        final String driver,
        final String keyField
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        return new KeySqlDeleteParser<>(table, keyField, dialect.quoteStart(), dialect.quoteEnd());
    }
}
