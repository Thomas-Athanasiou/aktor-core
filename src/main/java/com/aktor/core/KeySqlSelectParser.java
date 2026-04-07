package com.aktor.core;

import com.aktor.core.exception.ConversionException;
import com.aktor.core.model.FieldNormalizer;
import com.aktor.core.model.SqlDialect;
import com.aktor.core.model.SqlDialectResolver;

public final class KeySqlSelectParser<Key>
extends KeySqlParserBase<Key>
{
    private KeySqlSelectParser(final String table, final String keyField, final String start, final String end)
    {
        this(table, new String[] {keyField}, start, end);
    }

    private KeySqlSelectParser(final String table, final String[] keyFields, final String start, final String end)
    {
        super(table, keyFields, start, end);
    }

    @Override
    public String convert(final Key input) throws ConversionException
    {
        requireKey(input);
        return SqlParserUtil.selectByKeySql(table, keyFields, start, end);
    }

    public static <Key> KeySqlSelectParser<Key> of(
        final String table,
        final String driver,
        final FieldNormalizer fieldNormalizer
    )
    {
        return of(table, driver, fieldNormalizer.resolve(LOGICAL_KEY_FIELD_NAME));
    }

    private static <Key> KeySqlSelectParser<Key> of(
        final String table,
        final String driver,
        final String keyField
    )
    {
        final SqlDialect dialect = SqlDialectResolver.of(driver);
        return new KeySqlSelectParser<>(table, keyField, dialect.quoteStart(), dialect.quoteEnd());
    }
}
