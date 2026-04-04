package com.aktor.core;

import com.aktor.core.exception.ConversionException;

import java.util.Arrays;

abstract class KeySqlParserBase<Key>
extends SqlStatementParserBase
implements Converter<Key, String>
{
    protected final String[] keyFields;

    protected KeySqlParserBase(
        final String tableName,
        final String[] keyFields,
        final String start,
        final String end
    )
    {
        super(tableName, start, end);
        this.keyFields = Arrays.copyOf(SqlParserUtil.requireKeyFieldNames(keyFields), keyFields.length);
    }

    protected final void requireKey(final Key input) throws ConversionException
    {
        if (input == null)
        {
            throw new ConversionException("Key cannot be null");
        }
    }
}
