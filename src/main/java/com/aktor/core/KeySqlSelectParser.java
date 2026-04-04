package com.aktor.core;

import com.aktor.core.exception.ConversionException;

public class KeySqlSelectParser<Key>
extends KeySqlParserBase<Key>
{
    public KeySqlSelectParser(final String table, final String keyField, final String start, final String end)
    {
        this(table, new String[] {keyField}, start, end);
    }

    KeySqlSelectParser(final String table, final String[] keyFields, final String start, final String end)
    {
        super(table, keyFields, start, end);
    }

    @Override
    public String convert(final Key input) throws ConversionException
    {
        requireKey(input);
        return SqlParserUtil.selectByKeySql(table, keyFields, start, end);
    }
}
