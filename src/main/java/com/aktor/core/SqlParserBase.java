package com.aktor.core;

import com.aktor.core.exception.ConversionException;

import java.util.Objects;

abstract class SqlParserBase<Item>
extends SqlStatementParserBase
implements Converter<Item, String>
{
    private final Converter<Item, Row> converter;

    protected SqlParserBase(
        final String table,
        final String start,
        final String end,
        final Converter<Item, Row> converter
    )
    {
        super(table, start, end);
        this.converter = Objects.requireNonNull(converter);
    }

    protected final Value[] requireValues(final Item input) throws ConversionException
    {
        final Value[] values = converter.convert(input).values();
        if (values.length < 1)
        {
            throw new ConversionException("Data row values cannot be empty");
        }
        return values;
    }
}
