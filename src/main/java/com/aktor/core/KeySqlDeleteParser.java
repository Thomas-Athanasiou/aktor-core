package com.aktor.core;

import com.aktor.core.exception.ConversionException;

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
}
